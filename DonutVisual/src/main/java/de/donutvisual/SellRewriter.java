package de.donutvisual;

import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Schreibt $-Betraege in Chat-Nachrichten um, die den Item-Namen einer Regel enthalten.
 *  Haengt vom echten Nachrichtenformat des Servers ab -> ggf. anpassen. */
public class SellRewriter {
    private static final Pattern PRICE = Pattern.compile("\\$[\\d.,]+[KMBkmb]?");

    public static Text rewrite(Text msg, boolean overlay) {
        String s = msg.getString();
        String low = s.toLowerCase();
        for (FakeRule r : RuleManager.RULES) {
            if (r.sellPrice < 0) continue;
            String srcName = r.source.substring(r.source.indexOf(':') + 1).replace('_', ' ');
            boolean hit = low.contains(srcName) || (!r.name.isBlank() && low.contains(r.name.toLowerCase()));
            if (!hit) continue;
            Matcher m = PRICE.matcher(s);
            if (m.find()) {
                String out = m.replaceFirst(Matcher.quoteReplacement("$" + RuleManager.formatPrice(r.sellPrice)));
                return Text.literal(out).setStyle(msg.getStyle());
            }
        }
        return msg;
    }
}
