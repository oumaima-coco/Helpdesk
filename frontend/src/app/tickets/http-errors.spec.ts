import { errorKey } from './http-errors';

describe('errorKey', () => {
  it('maps each backend status to a translated message', () => {
    expect(errorKey(400)).toBe('errors.invalid');
    expect(errorKey(403)).toBe('errors.forbidden');
    expect(errorKey(404)).toBe('errors.notFound');
    expect(errorKey(409)).toBe('errors.conflict');
  });

  it('falls back to a generic message for anything else', () => {
    expect(errorKey(500)).toBe('common.error');
    expect(errorKey(0)).toBe('common.error');
  });
});
