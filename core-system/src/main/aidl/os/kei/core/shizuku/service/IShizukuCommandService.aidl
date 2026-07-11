package os.kei.core.shizuku.service;

import os.kei.core.shizuku.service.IShizukuCommandCallback;

interface IShizukuCommandService {
    void destroy() = 16777114;

    void execute(
        String commandId,
        String command,
        long timeoutMs,
        int maxOutputBytes,
        IShizukuCommandCallback callback
    ) = 1;

    void cancel(String commandId) = 2;

    int getServiceVersion() = 3;
}
