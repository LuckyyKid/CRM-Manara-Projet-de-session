package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = {"sender", "recipient", "conversation"})
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @EntityGraph(attributePaths = {"sender", "recipient", "conversation"})
    List<ChatMessage> findByConversationIdAndRecipientIdAndReadStatusFalse(Long conversationId, Long recipientId);

    long countByRecipientIdAndReadStatusFalse(Long recipientId);

    @Query("""
            SELECT m.conversation.id, COUNT(m)
            FROM ChatMessage m
            WHERE m.recipient.id = :recipientId
              AND m.readStatus = false
              AND m.conversation.id IN :conversationIds
            GROUP BY m.conversation.id
            """)
    List<Object[]> countUnreadByConversationIds(@Param("recipientId") Long recipientId,
                                                @Param("conversationIds") List<Long> conversationIds);
}
