import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DfaWeb {

    static final String DIGITS = "0123456789";

    static class Result {
        boolean ok;
        List<String> path;
        String reason;

        Result(boolean ok, List<String> path, String reason) {
            this.ok = ok;
            this.path = path;
            this.reason = reason;
        }
    }

    static class DFA {
        String start;
        Set<String> finals = new HashSet<>();
        Map<String, Map<Character, String>> delta = new HashMap<>();

        DFA(String start, String... finalStates) {
            this.start = start;
            finals.addAll(Arrays.asList(finalStates));
        }

        void add(String from, char c, String to) {
            delta.computeIfAbsent(from, k -> new HashMap<>()).put(c, to);
        }

        void addAll(String from, String chars, String to) {
            for (char c : chars.toCharArray())
                add(from, c, to);
        }

        Map<Character, String> out(String state) {
            return delta.getOrDefault(state, Collections.emptyMap());
        }

        Result run(String text) {
            String state = start;
            List<String> path = new ArrayList<>();
            path.add(state);
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                String next = out(state).get(ch);
                if (next == null) {
                    return new Result(false, path, "Rejected at position " + (i + 1)
                            + ": got '" + ch + "' in state " + state
                            + ", expected " + describe(out(state).keySet()) + ".");
                }
                state = next;
                path.add(state);
            }
            if (finals.contains(state))
                return new Result(true, path, "Accepted.");
            return new Result(false, path, "Input ended in non-final state " + state
                    + "; expected " + describe(out(state).keySet()) + " next.");
        }
    }

    static String describe(Set<Character> symbols) {
        if (symbols.isEmpty())
            return "nothing (final state)";
        boolean allDigits = true;
        for (char d : DIGITS.toCharArray())
            if (!symbols.contains(d))
                allDigits = false;
        if (allDigits)
            return "any digit 0-9";
        List<String> shown = new ArrayList<>();
        for (char c : new TreeSet<>(symbols))
            shown.add(c == ' ' ? "space" : "'" + c + "'");
        return String.join(" / ", shown);
    }

    // Optional +91, then 10 digits, first digit 6-9. Final state: q13
    static DFA phoneDfa() {
        DFA d = new DFA("q0", "q13");
        d.add("q0", '+', "q1");
        d.addAll("q0", "6789", "q4");
        d.add("q1", '9', "q2");
        d.add("q2", '1', "q3");
        d.addAll("q3", "6789", "q4");
        for (int i = 4; i <= 12; i++)
            d.addAll("q" + i, DIGITS, "q" + (i + 1));
        return d;
    }

    // 12 digits, first digit 2-9, optional space after 4th and 8th digit. Final:
    // q14
    static DFA aadhaarDfa() {
        DFA d = new DFA("q0", "q14");
        d.addAll("q0", "23456789", "q1");
        String[][] moves = {
                { "q1", "q2" }, { "q2", "q3" }, { "q3", "q4" }, { "q4", "q6" }, { "q5", "q6" },
                { "q6", "q7" }, { "q7", "q8" }, { "q8", "q9" }, { "q9", "q11" }, { "q10", "q11" },
                { "q11", "q12" }, { "q12", "q13" }, { "q13", "q14" }
        };
        for (String[] m : moves)
            d.addAll(m[0], DIGITS, m[1]);
        d.add("q4", ' ', "q5");
        d.add("q9", ' ', "q10");
        return d;
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String box(String label, Result r) {
        return "<div class=\"box " + (r.ok ? "ok" : "bad") + "\">"
                + "<strong>" + (r.ok ? "&#10003; " : "&#10007; ") + label + " is "
                + (r.ok ? "VALID" : "INVALID") + "</strong>"
                + "<div>" + esc(r.reason) + "</div>"
                + "<small>" + label + " DFA Path: " + esc(String.join(" \u2192 ", r.path)) + "</small>"
                + "</div>";
    }

    static final String PAGE = """
            <!doctype html>
            <html lang="en">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Phone &amp; Aadhaar Validator</title>
            <style>
            * { box-sizing: border-box; }
            body { margin: 0; font-family: Arial, sans-serif; background: #dfe3e4; color: #111; }
            header { background: #0f1b33; color: #fff; padding: 20px 28px; font-size: 20px; height: 130px; }
            .card { background: #f2f5f6; max-width: 560px; margin: -70px auto 40px; padding: 26px 30px; border-radius: 12px; box-shadow: 0 8px 24px rgba(0,0,0,.2); }
            .tag { text-align: center; letter-spacing: 3px; font-size: 13px; margin: 0; color: #5a1a1a; }
            h1 { text-align: center; color: #12264d; margin: 4px 0 18px; }
            label { display: block; font-weight: bold; margin: 12px 0 6px; font-size: 14px; }
            input { width: 100%; padding: 12px; border: 1px solid #b8bec0; border-radius: 4px; font-size: 16px; }
            button { width: 100%; margin-top: 18px; padding: 13px; background: #8b2830; color: #fff; border: 0; border-radius: 4px; font-weight: bold; font-size: 16px; cursor: pointer; }
            button:hover { background: #721f26; }
            .result { margin-top: 24px; border-left: 4px solid #8b2830; padding-left: 16px; }
            .result h2 { margin: 0 0 12px; }
            .box { background: #e8ecee; padding: 12px 14px; border-radius: 6px; margin-bottom: 12px; }
            .box small { display: block; margin-top: 8px; color: #444; word-break: break-word; }
            .ok strong { color: #2d6a2d; }
            .bad strong { color: #a11f2a; }
            </style>
            </head>
            <body>
            <header>Automata Theory &amp; Compiler Design</header>
            <main class="card">
            <p class="tag">DFA VALIDATION</p>
            <h1>Enter Details</h1>
            <form method="post">
            <label for="phone">Mobile Number</label>
            <input id="phone" name="phone" value="{{PHONE}}" placeholder="+919876543210" autocomplete="off">
            <label for="aadhaar">Aadhaar Number (use a dummy number)</label>
            <input id="aadhaar" name="aadhaar" value="{{AADHAAR}}" placeholder="2345 6789 0123" autocomplete="off">
            <button type="submit">VALIDATE</button>
            </form>
            {{RESULT}}
            </main>
            </body>
            </html>
            """;

    static Map<String, String> parseForm(String body) {
        Map<String, String> m = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty())
                continue;
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8),
                    URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);
        server.createContext("/", ex -> {
            String phone = "", aadhaar = "", result = "";
            if ("POST".equals(ex.getRequestMethod())) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> f = parseForm(body);
                phone = f.getOrDefault("phone", "").trim();
                aadhaar = f.getOrDefault("aadhaar", "").trim();
                result = "<section class=\"result\"><h2>Validation Result</h2>"
                        + box("Phone", phoneDfa().run(phone))
                        + box("Aadhaar", aadhaarDfa().run(aadhaar))
                        + "</section>";
            }
            String html = PAGE.replace("{{PHONE}}", esc(phone))
                    .replace("{{AADHAAR}}", esc(aadhaar))
                    .replace("{{RESULT}}", result);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
        System.out.println("Open http://127.0.0.1:5000 in your browser (Ctrl+C to stop)");
    }
}
