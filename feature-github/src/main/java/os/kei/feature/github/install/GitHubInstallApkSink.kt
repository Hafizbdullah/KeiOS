package os.kei.feature.github.install

import android.content.pm.PackageInstaller
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Destination the staged APK bytes are written to.
 *
 * The Shizuku backend writes straight into a privileged `PackageInstaller.Session`. The root backend
 * has no session binder to write into, so it lands the bytes in the app's own cache and hands the
 * path to `pm install-write` afterwards. Both share the download, progress, and archive handling in
 * [GitHubInstallSessionWriter].
 */
interface GitHubInstallApkSink {
    fun openWrite(name: String, offsetBytes: Long, lengthBytes: Long): OutputStream

    fun fsync(output: OutputStream)
}

internal class GitHubSessionApkSink(
    private val session: PackageInstaller.Session,
) : GitHubInstallApkSink {
    override fun openWrite(name: String, offsetBytes: Long, lengthBytes: Long): OutputStream =
        session.openWrite(name, offsetBytes, lengthBytes)

    override fun fsync(output: OutputStream) {
        session.fsync(output)
    }
}

/**
 * Collects the staged APK into [targetFile] and remembers the split name the writer chose, which
 * `pm install-write` needs to reproduce the same session layout.
 */
internal class GitHubFileApkSink(
    private val targetFile: File,
) : GitHubInstallApkSink {
    var splitName: String = DEFAULT_SPLIT_NAME
        private set

    val file: File
        get() = targetFile

    override fun openWrite(name: String, offsetBytes: Long, lengthBytes: Long): OutputStream {
        splitName = name.ifBlank { DEFAULT_SPLIT_NAME }
        return FileOutputStream(targetFile)
    }

    override fun fsync(output: OutputStream) {
        output.flush()
        if (output is FileOutputStream) {
            runCatching { output.fd.sync() }
        }
    }

    private companion object {
        const val DEFAULT_SPLIT_NAME = "base.apk"
    }
}
