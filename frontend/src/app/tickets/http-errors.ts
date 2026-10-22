import { TranslationKey } from '../i18n/translations';

export function errorKey(status: number): TranslationKey {
  switch (status) {
    case 400:
      return 'errors.invalid';
    case 403:
      return 'errors.forbidden';
    case 404:
      return 'errors.notFound';
    case 409:
      return 'errors.conflict';
    default:
      return 'common.error';
  }
}
