import { deadlineOf } from './deadline';

const NOW = new Date('2026-10-23T09:00:00Z');

describe('deadlineOf', () => {
  it('shows the remaining working hours while the clock is paused', () => {
    const deadline = deadlineOf(
      { pickupDueAt: null, resolutionDueAt: null, pausedRemainingSeconds: 48 * 3600 },
      NOW,
    );

    expect(deadline).toEqual({ kind: 'paused', dueAt: null, hoursLeft: 48, overdue: false });
  });

  it('uses the resolution deadline once an agent has the ticket', () => {
    const deadline = deadlineOf(
      { pickupDueAt: null, resolutionDueAt: '2026-10-27T09:00:00Z', pausedRemainingSeconds: null },
      NOW,
    );

    expect(deadline?.kind).toBe('resolve');
    expect(deadline?.overdue).toBe(false);
  });

  it('flags a deadline in the past as overdue', () => {
    const deadline = deadlineOf(
      { pickupDueAt: '2026-10-22T09:00:00Z', resolutionDueAt: null, pausedRemainingSeconds: null },
      NOW,
    );

    expect(deadline).toEqual({ kind: 'pickup', dueAt: '2026-10-22T09:00:00Z', hoursLeft: null, overdue: true });
  });

  it('has nothing to show for resolved or closed tickets', () => {
    expect(deadlineOf({ pickupDueAt: null, resolutionDueAt: null, pausedRemainingSeconds: null }, NOW)).toBeNull();
  });
});
