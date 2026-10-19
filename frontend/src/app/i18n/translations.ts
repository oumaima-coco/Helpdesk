const fr = {
  'app.title': 'Helpdesk',
  'header.language': 'Langue',
  'header.logout': 'Se déconnecter',
};

type TranslationKey = keyof typeof fr;
type Dictionary = Record<TranslationKey, string>;

const ar: Dictionary = {
  'app.title': 'مكتب المساعدة',
  'header.language': 'اللغة',
  'header.logout': 'تسجيل الخروج',
};

const en: Dictionary = {
  'app.title': 'Helpdesk',
  'header.language': 'Language',
  'header.logout': 'Log out',
};

export type Language = 'fr' | 'ar' | 'en';
export type { TranslationKey };

export const languages: { code: Language; label: string }[] = [
  { code: 'fr', label: 'Français' },
  { code: 'ar', label: 'العربية' },
  { code: 'en', label: 'English' },
];

export const translations: Record<Language, Dictionary> = { fr, ar, en };
