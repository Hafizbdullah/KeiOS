package os.kei.core.shizuku.service;

oneway interface IShizukuCommandCallback {
    void onSnapshot(String stdout, String stderr, boolean stdoutTruncated, boolean stderrTruncated);

    void onCompleted(
        String stdout,
        String stderr,
        int exitCode,
        boolean hasExitCode,
        boolean timedOut,
        boolean cancelled,
        boolean stdoutTruncated,
        boolean stderrTruncated
    );
}
