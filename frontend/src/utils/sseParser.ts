import type { ParsedSseEvent, SseEventType } from '@/types';

/**
 * Stateful SSE parser that handles chunked network delivery.
 *
 * The Fetch ReadableStream delivers data at network boundaries, not at
 * SSE frame boundaries. A single chunk may contain multiple events, or
 * a single event may be split across two chunks.
 *
 * This parser maintains a line buffer across calls and emits complete
 * events as they become fully available.
 *
 * SSE frame format (simplified):
 *   event: <name>\n
 *   data: <json>\n
 *   \n         ← blank line = frame boundary
 */
export class SseParser {
  private buffer = '';

  /**
   * Feed a raw text chunk and get back all complete events found in it.
   */
  parse(chunk: string): ParsedSseEvent[] {
    this.buffer += chunk;
    const events: ParsedSseEvent[] = [];

    // Split on double newline (SSE frame boundary)
    const frames = this.buffer.split('\n\n');

    // Keep the last (potentially incomplete) frame in the buffer
    this.buffer = frames.pop() ?? '';

    for (const frame of frames) {
      const event = this.parseFrame(frame.trim());
      if (event) {
        events.push(event);
      }
    }

    return events;
  }

  private parseFrame(frame: string): ParsedSseEvent | null {
    if (!frame) return null;

    let eventName: string | null = null;
    let data: string | null = null;

    for (const line of frame.split('\n')) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        data = line.slice(5).trim();
      }
    }

    if (eventName && data !== null) {
      return { event: eventName as SseEventType, data };
    }

    return null;
  }

  /** Reset internal buffer (call when starting a new stream). */
  reset(): void {
    this.buffer = '';
  }
}

// Convenience singleton for one-off use
export const createSseParser = () => new SseParser();
