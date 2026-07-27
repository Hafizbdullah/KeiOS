package os.kei.core.privilege

import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RootShellCommandRunnerTest {
    @Test
    fun `script prints the shell pid before running the command`() {
        val script = RootShellCommandRunner.buildScript("getprop ro.build.version.sdk")

        val lines = script.trimEnd().lines()
        assertEquals("printf '${RootShellCommandRunner.PID_MARKER}%s\\n' \"$$\"", lines.first())
        assertEquals("getprop ro.build.version.sdk", lines.last())
    }

    @Test
    fun `script keeps multi line commands intact`() {
        val command = "for i in 1 2 3; do\n  echo \$i\ndone"

        val script = RootShellCommandRunner.buildScript(command)

        assertContains(script, command)
        assertTrue(script.endsWith("\n"))
    }

    @Test
    fun `kill script targets the group then the direct children then the shell`() {
        val script = RootShellCommandRunner.buildKillScript(4242)

        assertEquals(
            listOf(
                "kill -9 -4242 2>/dev/null",
                "pkill -9 -P 4242 2>/dev/null",
                "kill -9 4242 2>/dev/null",
            ),
            script.trimEnd().lines(),
        )
    }

    @Test
    fun `pid marker is consumed and never reaches the output`() {
        val stream = "${RootShellCommandRunner.PID_MARKER}1234\nhello\n".byteInputStream()

        val handshake = stream.readPidMarker()

        assertEquals(1234, handshake.pid)
        assertEquals(0, handshake.leftover.size)
        assertEquals("hello\n", stream.readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun `output survives when the shell never printed a marker`() {
        val stream = "some output\nmore\n".byteInputStream()

        val handshake = stream.readPidMarker()

        assertNull(handshake.pid)
        assertEquals("some output\n", handshake.leftover.toString(Charsets.UTF_8))
        assertEquals("more\n", stream.readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun `marker scan stops at the limit instead of draining the stream`() {
        val noise = "x".repeat(64)
        val stream = ByteArrayInputStream("$noise\ntail\n".toByteArray())

        val handshake = stream.readPidMarker(scanLimit = 8)

        assertNull(handshake.pid)
        assertEquals(8, handshake.leftover.size)
    }

    @Test
    fun `probe uid reads the first numeric line`() {
        assertEquals(0, parseProbeUid("0\n"))
        assertEquals(2000, parseProbeUid("  2000  \n"))
        assertEquals(0, parseProbeUid("warning: something\n0\n"))
        assertNull(parseProbeUid("Permission denied\n"))
        assertNull(parseProbeUid(""))
    }

    @Test
    fun `bounded output flags truncation past the cap`() {
        val output = BoundedOutput(maxBytes = 4)

        output.append("abcdef")

        assertEquals("abcd", output.text())
        assertTrue(output.truncated)
    }

    @Test
    fun `bounded output keeps short writes untouched`() {
        val output = BoundedOutput(maxBytes = 16)

        output.append("ok")

        assertEquals("ok", output.text())
        assertTrue(!output.truncated)
    }
}
