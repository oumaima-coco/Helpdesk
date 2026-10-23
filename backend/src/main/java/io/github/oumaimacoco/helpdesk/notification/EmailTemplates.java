package io.github.oumaimacoco.helpdesk.notification;

import static java.util.Map.entry;

import java.util.Map;

final class EmailTemplates {

    private static final String DEFAULT_LANGUAGE = "fr";

    private static final Map<String, String> SUBJECTS = Map.of(
            "fr", "[Helpdesk] %s : %s",
            "ar", "[Helpdesk] %s: %s",
            "en", "[Helpdesk] %s: %s");

    private static final Map<String, String> BODIES = Map.of(
            "fr", """
                    Bonjour %s,

                    %s : ticket %s.

                    Connectez-vous au helpdesk pour voir les détails :
                    %s

                    Cet e-mail est envoyé automatiquement, merci de ne pas y répondre.
                    """,
            "ar", """
                    مرحبًا %s،

                    %s: التذكرة %s.

                    يرجى الدخول إلى مكتب المساعدة للاطلاع على التفاصيل:
                    %s

                    أُرسلت هذه الرسالة آليًا، يرجى عدم الرد عليها.
                    """,
            "en", """
                    Hello %s,

                    %s: ticket %s.

                    Please sign in to the helpdesk to see the details:
                    %s

                    This email was sent automatically, please do not reply.
                    """);

    private static final Map<String, Map<EmailReason, String>> PHRASES = Map.of(
            "fr", Map.ofEntries(
                    entry(EmailReason.TICKET_HANDLED, "Votre ticket est pris en charge"),
                    entry(EmailReason.ASSIGNED_TO_YOU, "Un ticket vous a été assigné"),
                    entry(EmailReason.WORK_STARTED, "Le travail sur votre ticket a commencé"),
                    entry(EmailReason.INFO_REQUESTED, "Des informations vous sont demandées"),
                    entry(EmailReason.REQUESTER_REPLIED, "Le demandeur a répondu"),
                    entry(EmailReason.RESOLVED, "Votre ticket a été résolu, merci de confirmer"),
                    entry(EmailReason.CONFIRMED, "Le demandeur a confirmé la résolution"),
                    entry(EmailReason.REOPENED, "Le demandeur a rouvert le ticket"),
                    entry(EmailReason.WAITING_FOR_AGENT, "Votre ticket attend un autre agent"),
                    entry(EmailReason.ESCALATED_TO_TEAM, "Un ticket a été escaladé et attend un agent"),
                    entry(EmailReason.OVERDUE_AGENT, "Un ticket qui vous était assigné a dépassé son délai et a été remis dans la file"),
                    entry(EmailReason.OVERDUE_TEAM, "Un ticket a dépassé son délai et a été remis dans la file"),
                    entry(EmailReason.NOT_PICKED_UP, "Un ticket attend d'être pris en charge depuis trop longtemps")),
            "ar", Map.ofEntries(
                    entry(EmailReason.TICKET_HANDLED, "تتم معالجة تذكرتك"),
                    entry(EmailReason.ASSIGNED_TO_YOU, "أُسندت إليك تذكرة"),
                    entry(EmailReason.WORK_STARTED, "بدأ العمل على تذكرتك"),
                    entry(EmailReason.INFO_REQUESTED, "مطلوب منك معلومات إضافية"),
                    entry(EmailReason.REQUESTER_REPLIED, "ردّ صاحب الطلب"),
                    entry(EmailReason.RESOLVED, "تم حل تذكرتك، يرجى التأكيد"),
                    entry(EmailReason.CONFIRMED, "أكّد صاحب الطلب الحل"),
                    entry(EmailReason.REOPENED, "أعاد صاحب الطلب فتح التذكرة"),
                    entry(EmailReason.WAITING_FOR_AGENT, "تذكرتك بانتظار وكيل آخر"),
                    entry(EmailReason.ESCALATED_TO_TEAM, "صُعّدت تذكرة وهي بانتظار وكيل"),
                    entry(EmailReason.OVERDUE_AGENT, "تجاوزت تذكرة كانت مسندة إليك مهلتها وأُعيدت إلى قائمة الانتظار"),
                    entry(EmailReason.OVERDUE_TEAM, "تجاوزت تذكرة مهلتها وأُعيدت إلى قائمة الانتظار"),
                    entry(EmailReason.NOT_PICKED_UP, "تذكرة تنتظر التولّي منذ مدة طويلة")),
            "en", Map.ofEntries(
                    entry(EmailReason.TICKET_HANDLED, "Your ticket is being handled"),
                    entry(EmailReason.ASSIGNED_TO_YOU, "A ticket has been assigned to you"),
                    entry(EmailReason.WORK_STARTED, "Work on your ticket has started"),
                    entry(EmailReason.INFO_REQUESTED, "Information is requested from you"),
                    entry(EmailReason.REQUESTER_REPLIED, "The requester has replied"),
                    entry(EmailReason.RESOLVED, "Your ticket has been resolved, please confirm"),
                    entry(EmailReason.CONFIRMED, "The requester confirmed the resolution"),
                    entry(EmailReason.REOPENED, "The requester reopened the ticket"),
                    entry(EmailReason.WAITING_FOR_AGENT, "Your ticket is waiting for another agent"),
                    entry(EmailReason.ESCALATED_TO_TEAM, "A ticket was escalated and needs an agent"),
                    entry(EmailReason.OVERDUE_AGENT, "A ticket you had passed its deadline and was returned to the queue"),
                    entry(EmailReason.OVERDUE_TEAM, "A ticket passed its deadline and was returned to the queue"),
                    entry(EmailReason.NOT_PICKED_UP, "A ticket has been waiting too long to be picked up")));

    private EmailTemplates() {
    }

    static String subject(String language, EmailReason reason, String reference) {
        String lang = supported(language);
        return SUBJECTS.get(lang).formatted(reference, PHRASES.get(lang).get(reason));
    }

    static String body(String language, EmailReason reason, String name, String reference, String link) {
        String lang = supported(language);
        return BODIES.get(lang).formatted(name, PHRASES.get(lang).get(reason), reference, link);
    }

    private static String supported(String language) {
        return BODIES.containsKey(language) ? language : DEFAULT_LANGUAGE;
    }
}