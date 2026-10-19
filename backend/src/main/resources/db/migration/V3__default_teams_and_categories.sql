INSERT INTO team (name) VALUES
    ('Support Desk'),
    ('Network & Infrastructure'),
    ('Business Applications');

INSERT INTO category (name_fr, name_ar, name_en, team_id) VALUES
    ('Comptes et mots de passe', 'الحسابات وكلمات المرور', 'Accounts and passwords', (SELECT id FROM team WHERE name = 'Support Desk')),
    ('Poste de travail', 'محطة العمل', 'Workstation', (SELECT id FROM team WHERE name = 'Support Desk')),
    ('Imprimantes', 'الطابعات', 'Printers', (SELECT id FROM team WHERE name = 'Support Desk')),
    ('Messagerie', 'البريد الإلكتروني', 'Email', (SELECT id FROM team WHERE name = 'Support Desk')),
    ('Réseau et VPN', 'الشبكة و VPN', 'Network and VPN', (SELECT id FROM team WHERE name = 'Network & Infrastructure')),
    ('Applications métier', 'تطبيقات العمل', 'Business applications', (SELECT id FROM team WHERE name = 'Business Applications')),
    ('Autre', 'أخرى', 'Other', (SELECT id FROM team WHERE name = 'Support Desk'));