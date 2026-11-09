import { getApiBaseUrl } from './api';
import { SseParser } from '@/utils/sseParser';
import type {
  SseDoneEvent,
  SseErrorEvent,
  SseStatusEvent,
  SseTokenEvent,
} from '@/types';

export interface StreamCallbacks {
  onThinking?: (event: SseStatusEvent) => void;
  onToken: (event: SseTokenEvent) => void;
  onDone: (event: SseDoneEvent) => void;
  onError: (event: SseErrorEvent | Error) => void;
}

/**
 * Streams a chat message to the backend SSE endpoint.
 *
 * Uses fetch() instead of native EventSource because:
 * - EventSource only supports GET and cannot send Authorization headers
 * - We need a POST body for the message content
 * - The spec explicitly requires fetch() + ReadableStream
 *
 * Returns an AbortController that the caller can use to cancel the stream.
 */
export function streamMessage(
  conversationId: number,
  message: string,
  callbacks: StreamCallbacks
): AbortController {
  const controller = new AbortController();
  const parser = new SseParser();
  const token = sessionStorage.getItem('auth_token');

  const url = `${getApiBaseUrl()}/chat/${conversationId}/stream`;

  fetch(url, {
    method: 'POST',
    signal: controller.signal,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${token ?? ''}`,
    },
    body: JSON.stringify({ message }),
  })
    .then(async (response) => {
      if (!response.ok) {
        const body = await response.text().catch(() => '');
        callbacks.onError(
          new Error(`HTTP ${response.status}: ${body || response.statusText}`)
        );
        return;
      }

      if (!response.body) {
        callbacks.onError(new Error('No response body for SSE stream'));
        return;
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      const pump = async () => {
        try {
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });
            const events = parser.parse(chunk);

            for (const { event, data } of events) {
              try {
                const parsed = JSON.parse(data);
                switch (event) {
                  case 'status':
                    callbacks.onThinking?.(parsed as SseStatusEvent);
                    break;
                  case 'token':
                    callbacks.onToken(parsed as SseTokenEvent);
                    break;
                  case 'done':
                    callbacks.onDone(parsed as SseDoneEvent);
                    break;
                  case 'error':
                    callbacks.onError(parsed as SseErrorEvent);
                    break;
                }
              } catch {
                // Malformed JSON in event data — skip
              }
            }
          }
        } catch (err) {
          if (err instanceof Error && err.name === 'AbortError') {
            // User cancelled — not an error
            return;
          }
          callbacks.onError(err instanceof Error ? err : new Error(String(err)));
        }
      };

      await pump();
    })
    .catch((err: unknown) => {
      if (err instanceof Error && err.name === 'AbortError') return;
      callbacks.onError(err instanceof Error ? err : new Error(String(err)));
    });

  return controller;
}
