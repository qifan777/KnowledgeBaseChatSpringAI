package io.github.qifan777.knowledge.ai.message;

import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class AiMessageChatMemory implements ChatMemory {
    private final AiMessageRepository messageRepository;

    /**
     * 不实现，手动前端发起请求保存用户的消息和大模型回复的消息
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
    }

    /**
     * 查询会话内的消息最新n条历史记录
     *
     * @param conversationId 会话id
     * @param lastN          最近n条
     * @return org.springframework.ai.chat.messages.Message格式的消息
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        return messageRepository
                // 查询会话内的最新n条消息
                .findBySessionId(conversationId, lastN)
                .stream()
                // 转成Message对象
                .map(AiMessageChatMemory::toSpringAiMessage)
                .toList();
    }

    /**
     * 清除会话内的消息
     *
     * @param conversationId 会话id
     */
    @Override
    public void clear(String conversationId) {
        messageRepository.deleteBySessionId(conversationId);
    }

    public static AiMessage toAiMessage(Message message, String sessionId) {
        return AiMessageDraft.$.produce(draft -> {
            draft.setSessionId(sessionId)
                    .setTextContent(message.getContent())
                    .setType(message.getMessageType())
                    .setMedias(new ArrayList<>());
            if (!CollectionUtil.isEmpty(message.getMedia())) {
                List<AiMessage.Media> mediaList = message
                        .getMedia()
                        .stream()
                        .map(media -> new AiMessage.Media(media.getMimeType().getType(), media.getData().toString()))
                        .toList();
                draft.setMedias(mediaList);
            }
        });
    }

    public static Message toSpringAiMessage(AiMessage aiMessage) {
        List<Media> mediaList = new ArrayList<>();
        if (!CollectionUtil.isEmpty(aiMessage.medias())) {
            mediaList = aiMessage.medias().stream().map(AiMessageChatMemory::toSpringAiMedia).toList();
        }
        return new DefaultMessage(aiMessage.type(), aiMessage.textContent(), mediaList);
    }

    @SneakyThrows
    public static Media toSpringAiMedia(AiMessage.Media media) {
        return new Media(new MediaType(media.getType()), new URL(media.getData()));
    }

    public static class DefaultMessage extends AbstractMessage {
        protected DefaultMessage(MessageType messageType, String textContent, List<Media> media) {
            super(messageType, textContent, media);
        }
    }
}
