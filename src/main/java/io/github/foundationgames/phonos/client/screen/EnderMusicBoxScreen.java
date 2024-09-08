package io.github.foundationgames.phonos.client.screen;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import io.github.foundationgames.phonos.client.screen.widgets.EnderMusicBoxStreamList;
import io.github.foundationgames.phonos.network.ClientPayloadPackets;
import io.github.foundationgames.phonos.sound.custom.ClientCustomAudioUploader;
import io.github.foundationgames.phonos.sound.stream.AudioDataQueue;
import io.github.foundationgames.phonos.sound.stream.AudioFileUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnderMusicBoxScreen extends Screen {
    private static final Text TITLE = Text.translatable("container.phonos.ender_music_box");
    private static final Text DRAG_PROMPT = Text.translatable("hint.phonos.ender_music_box.drag").formatted(Formatting.GRAY, Formatting.ITALIC);
    private static final Text FULL_PROMPT = Text.translatable("hint.phonos.ender_music_box.drag.full").formatted(Formatting.GRAY, Formatting.ITALIC);

    private final EnderMusicBoxBlockEntity be;

    private EnderMusicBoxStreamList streamList;

    private Text fileName = DRAG_PROMPT;
    private Text status = Text.empty();
    private int statusExpiration = 0;
    private AudioDataQueue toUpload;

    public EnderMusicBoxScreen(EnderMusicBoxBlockEntity be) {
        super(TITLE);

        this.be = be;
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(this.streamList = new EnderMusicBoxStreamList(
            this.client,
            this.width, this.height,
            25, this.height - 40,
            20
        ));
        this.streamList.setRenderSelection(false);

        this.fileName = getDragPrompt();

        updateStreamList();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void updateStreamList() {
        Map<Long, EnderMusicBoxStreamList.Entry> existing = new HashMap<>();
        for (var entry : this.streamList.children()) {
            existing.put(entry.id, entry);
        }

        this.streamList.children().clear();

        be.forEachStream((id, name) -> {
            EnderMusicBoxStreamList.Entry entry;
            if (existing.containsKey(id)) {
                entry = existing.remove(id);
            } else {
                entry = new EnderMusicBoxStreamList.Entry(be, id, name, textRenderer);
            }
            this.streamList.children().add(entry);
        });

        this.streamList.children().forEach(EnderMusicBoxStreamList.Entry::tick);
    }

    private Text getDragPrompt() {
        if (toUpload != null)
            return Text.empty();

        return be.hasFreeSpace() ? DRAG_PROMPT : FULL_PROMPT;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, this.fileName, this.width / 2, this.height - 30, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, this.status, this.width / 2, this.height - 20, 0xAAAAAA);
    }

    @Override
    public void tick() {
        super.tick();

        if (statusExpiration > 0) {
            statusExpiration--;
            if (statusExpiration == 0) {
                this.status = Text.empty();
            }
        }

        if (toUpload == null && statusExpiration <= 0)
            this.fileName = getDragPrompt();

        updateStreamList();
    }

    @Override
    public void filesDragged(List<Path> paths) {
        super.filesDragged(paths);

        if (toUpload != null)
            return;

        if (!paths.isEmpty()) {
            var path = paths.get(0);

            try (var in = Files.newInputStream(path)) {
                var aud = AudioFileUtil.dataOfVorbis(in);
                if (aud != null) {
                    this.toUpload = aud;
                    this.fileName = Text.literal(path.getFileName().toString());
                    this.status = Text.translatable("status.phonos.ender_music_box.ready_upload").formatted(Formatting.GREEN);

                    ClientPayloadPackets.sendRequestEnderMusicBoxUploadSession(be, path.getFileName().toString());
                } else {
                    this.status = Text.translatable("status.phonos.ender_music_box.mono_only").formatted(Formatting.GOLD);
                    this.fileName = getDragPrompt();
                    this.toUpload = null;
                }
            } catch (IOException ex) {
                Phonos.LOG.error("Error reading ogg file " + path, ex);
                this.status = Text.translatable("status.phonos.ender_music_box.invalid_format").formatted(Formatting.RED);
                this.fileName = getDragPrompt();
                this.toUpload = null;
            }
        }
    }

    public void onAudioUploadStatus(long id, boolean ok) {
        if (ok) {
            if (toUpload != null) {
                this.status = Text.translatable("status.phonos.ender_music_box.uploading").formatted(Formatting.YELLOW);

                ClientCustomAudioUploader.queueForUpload(id, toUpload);
            } else {
                this.status = Text.empty();
            }
        } else {
            this.status = Text.translatable("status.phonos.ender_music_box.upload_denied").formatted(Formatting.RED);
            this.fileName = getDragPrompt();
            this.toUpload = null;
        }
    }

    public void onAudioUploadCancel(Text message) {
        this.status = message.copy().formatted(Formatting.RED);
        this.fileName = getDragPrompt();
        this.toUpload = null;
    }

    public void onFinishUpload() {
        this.status = Text.translatable("status.phonos.ender_music_box.uploaded").formatted(Formatting.GREEN);
        this.statusExpiration = 60;
        this.toUpload = null;
    }
}
