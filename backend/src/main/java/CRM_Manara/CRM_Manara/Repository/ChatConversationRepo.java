package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.ChatConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatConversationRepo extends JpaRepository<ChatConversation, Long> {

    @EntityGraph(attributePaths = {"participantOne", "participantTwo"})
    Optional<ChatConversation> findByParticipantOneIdAndParticipantTwoId(Long participantOneId, Long participantTwoId);

    @EntityGraph(attributePaths = {"participantOne", "participantTwo"})
    @Query("""
            SELECT c
            FROM ChatConversation c
            WHERE c.participantOne.id = :userId OR c.participantTwo.id = :userId
            ORDER BY c.updatedAt DESC
            """)
    List<ChatConversation> findAllForUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"participantOne", "participantTwo"})
    @Query("""
            SELECT c
            FROM ChatConversation c
            WHERE c.id = :conversationId
            """)
    Optional<ChatConversation> findDetailedById(@Param("conversationId") Long conversationId);
}
