
/*
 * @test
 * @summary Test process reaper threads in MT mode
 * @library /test/lib
 * @run main/othervm/timeout=20 -XX:+MultiTenant TestTenantProcessReaper
 */

import static jdk.test.lib.Asserts.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import jdk.test.lib.process.ProcessTools;
import com.alibaba.tenant.TenantConfiguration;
import com.alibaba.tenant.TenantContainer;
import com.alibaba.tenant.TenantException;

public class TestTenantProcessReaper {

    // 1M = 1024 * 1024 bytes
    private static final int M = 1024 * 1024;

    public static void main(String[] args) throws TenantException, InterruptedException, IOException {
        TenantContainer tenant = TenantContainer.create(new TenantConfiguration());
        AtomicBoolean childEnded = new AtomicBoolean(false);
        Process[] procHolder = new Process[1];

        // Start a child process from 'TenantContainer.run()', but the 'process reaper' threads should be created in
        // ROOT tenant
        tenant.run(() -> {
            ProcessBuilder pb = null;
            try {
                pb = ProcessTools.createJavaProcessBuilder(Inner.class.getName());
                procHolder[0] = pb.start();
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        });

        // the watcher thread to update value of 'childEnded'
        Thread procWatcher = new Thread(() -> {
            try {
                while (true) {
                    try {
                        procHolder[0].waitFor();
                        break;
                    } catch (InterruptedException e) {
                        // ignore and continue
                    }
                }
                childEnded.set(true);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        });
        procWatcher.start();

        // wait for child process to start
        SocketChannel pipe = NamedPipe.serverEnd();
        assertNotNull(pipe);
        pipe.read(ByteBuffer.allocate(1));

        // in the problematic case, below statement will kill the 'process reaper' threads accidentally
        // thus afterwards calls to 'Process.waitFor()' never return
        tenant.destroy();
        pipe.write(ByteBuffer.allocate(1));

        try {
            // wait for child process to try to end
            pipe.read(ByteBuffer.allocate(1));
            Thread.sleep(3_000);
            assertTrue(childEnded.get());
        } finally {
            if (procHolder[0].isAlive()) {
                procHolder[0].destroyForcibly();
            }
            procWatcher.join();
        }
    }

    public static class Inner {
        public static void main(String[] args) throws InterruptedException, IOException {
            SocketChannel pipe = null;
            while (pipe == null) {
                pipe = NamedPipe.clientEnd();
                Thread.sleep(200);
            }
            assertNotNull(pipe);

            // connected!

            pipe.write(ByteBuffer.allocate(1));
            pipe.read(ByteBuffer.allocate(1));

            // OK, parent has called 'TenantContainer.destroy()', we may quite now

            pipe.write(ByteBuffer.allocate(1));
        }
    }

    // create a 'named pipe' for Inter Process Communication
    static class NamedPipe {
        static final int PORT = 9988;

        static SocketChannel serverEnd() {
            try {
                ServerSocketChannel server = ServerSocketChannel.open();
                server.bind(new InetSocketAddress(InetAddress.getLocalHost(), PORT));
                server.configureBlocking(true);
                return server.accept();
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
            return null;
        }

        static SocketChannel clientEnd() {
            try {
                SocketChannel client = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), PORT));
                client.configureBlocking(true);
                return client;
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
            return null;
        }
    }
}
