package ru.daniilazarnov;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(ServerRequestHandler.class.getName());

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RequestMessage) {
            RequestMessage request = (RequestMessage) msg;
            String clientCmd = request.getCmd();

            switch (clientCmd) {
                case "download":
                    try {
                        if (Files.exists(Paths.get("./project/server_vault/" + request.getLogin() + "/" + request.getFilename()))) {
                            File file = new File("./project/server_vault/" + request.getLogin() + "/" + request.getFilename());
                            int bufSize = 1024 * 1024 * 10;
                            int partsCount = (int) (file.length() / bufSize);
                            if (file.length() % bufSize != 0) {
                                partsCount++;
                            }

                            FileMessage fm = new FileMessage(file.getName(), -1, partsCount, new byte[bufSize], request.getLogin());
                            FileInputStream in = new FileInputStream(file);

                            logger.debug("Downloading started");
                            for (int i = 0; i < partsCount; i++) {
                                int readedBytes = in.read(fm.getData());
                                fm.setPartNumber(i + 1);
                                if (readedBytes < bufSize) {
                                    fm.setData(Arrays.copyOfRange(fm.getData(), 0, readedBytes));
                                }
                                ctx.writeAndFlush(fm);
                                logger.debug("Part: " + (i + 1) + " was sent");
                                System.out.println(fm.getPartNumber());
                            }
                            in.close();
                            logger.debug("File was fully downloaded by client");
                        }
                    } catch (IOException e) {
                        logger.error("SWW while sending file for client", e);
                        throw new RuntimeException("SWW", e);
                    }
                    break;
                case "list":
                    logger.debug("Command LIST from client was received");
                    List<String> files = new ArrayList<>();
                    Files.walkFileTree(Paths.get("./project/server_vault/" + request.getLogin()),
                            new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    files.add(file.getFileName().toString());
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                    DirectoryInfoMessage dim = new DirectoryInfoMessage(files);
                    ctx.writeAndFlush(dim);
                    break;
                case "remove":
                    logger.debug("Command REMOVE from client was received");
                    Path removeDir = Paths.get("./project/server_vault/" + request.getLogin() + "/" + request.getFilename());
                    if (Files.exists(removeDir)) {
                        removeDir.toFile().delete();
                    }
                    break;
                case "rename":
                    logger.debug("Command RENAME from client was received");
                    Path renameDir = Paths.get("./project/server_vault/" + request.getLogin() + "/" + request.getFilename());
                    Path newDir = Paths.get("./project/server_vault/" + request.getLogin() + "/" + request.getNewFileName());
                    if (Files.exists(renameDir)) {
                        renameDir.toFile().renameTo(newDir.toFile());
                    }
                    break;
            }
        }
        else ctx.fireChannelRead(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client connected");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client disconnected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("SWW at auth handler", cause);
        super.exceptionCaught(ctx, cause);
    }
}
