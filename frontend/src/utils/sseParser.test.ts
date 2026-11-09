import { describe, it, expect, beforeEach } from 'vitest';
import { SseParser } from './sseParser';

describe('SseParser', () => {
  let parser: SseParser;

  beforeEach(() => {
    parser = new SseParser();
  });

  it('parses a complete SSE frame in one chunk', () => {
    const events = parser.parse('event: token\ndata: {"content":"Hello"}\n\n');
    expect(events).toHaveLength(1);
    expect(events[0]).toEqual({ event: 'token', data: '{"content":"Hello"}' });
  });

  it('handles multiple events in one chunk', () => {
    const chunk = [
      'event: status\ndata: {"status":"THINKING"}\n\n',
      'event: token\ndata: {"content":"Hi"}\n\n',
    ].join('');
    const events = parser.parse(chunk);
    expect(events).toHaveLength(2);
    expect(events[0].event).toBe('status');
    expect(events[1].event).toBe('token');
  });

  it('handles an event split across two chunks', () => {
    const chunk1 = 'event: token\ndat';
    const chunk2 = 'a: {"content":"World"}\n\n';
    const events1 = parser.parse(chunk1);
    expect(events1).toHaveLength(0); // incomplete

    const events2 = parser.parse(chunk2);
    expect(events2).toHaveLength(1);
    expect(events2[0]).toEqual({ event: 'token', data: '{"content":"World"}' });
  });

  it('handles done event', () => {
    const events = parser.parse('event: done\ndata: {"messageId":1,"status":"COMPLETED"}\n\n');
    expect(events).toHaveLength(1);
    expect(events[0].event).toBe('done');
  });

  it('handles error event', () => {
    const events = parser.parse('event: error\ndata: {"code":"LLM_ERROR","message":"fail"}\n\n');
    expect(events).toHaveLength(1);
    expect(events[0].event).toBe('error');
  });

  it('resets buffer on reset()', () => {
    parser.parse('event: token\ndat');
    parser.reset();
    const events = parser.parse('a: {"content":"x"}\n\n');
    // After reset, previous incomplete frame is gone
    expect(events).toHaveLength(0);
  });

  it('ignores empty chunks', () => {
    const events = parser.parse('');
    expect(events).toHaveLength(0);
  });

  it('handles a frame with only data line (no event line)', () => {
    // Should NOT emit an event since we need the named event
    const events = parser.parse('data: {"content":"x"}\n\n');
    expect(events).toHaveLength(0);
  });
});
