# ---------- KeiOS app R8 rules ----------
#
# Deliberately small. An audit of the release build found the configuration already sound without any
# app-level rules: R8 emits no missing_rules.txt, kotlinx-serialization 1.11.0's own consumer rules keep
# every generated serializer and the reflective companion lookup, `-keepattributes Signature` arrives
# through the OkHttp/Ktor/coroutines rules so top-level generic decodes such as
# `decodeFromString<List<BaAccountRecord>>` resolve, and every manifest component survives. The only
# reflection in app code targets platform classes that are not in our dex and therefore cannot be
# renamed. Nothing below is a workaround for a break; do not grow this file speculatively.

# Keep the *names* of throwables, and only the names.
#
# The app shows `javaClass.simpleName` to the teacher in about twenty places, and two of them are not
# cosmetic: FeedbackIssueRepository and FeedbackIssueViewModel put it in the body of the GitHub issue
# they file. Obfuscated, that reads `a43` — measured, not guessed: DownloadSizeMismatchException maps to
# exactly that in the release build. A submitted report naming `a43` cannot be triaged, and no mapping
# file is attached to it.
#
# `-keepnames` keeps the name but not the members, so these classes still shrink and their methods are
# still renamed; the cost is the string table entry. Everything else in the app stays obfuscated.
-keepnames class * extends java.lang.Throwable
