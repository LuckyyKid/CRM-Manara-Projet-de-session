package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnimateurService {

    @Autowired
    private AnimateurRepo animateurRepo;

    @Autowired
    private AnimationRepo animationRepo;

    @Autowired
    private InscriptionRepo inscriptionRepo;

    @Transactional(readOnly = true)
    public Animateur getAnimateurByEmail(String email) {
        return animateurRepo.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Animateur introuvable pour cet email"));
    }

    @Transactional(readOnly = true)
    public List<Animation> getAnimationsForAnimateur(Long animateurId) {
        return animationRepo.findByAnimateurId(animateurId);
    }

    @Transactional(readOnly = true)
    public long countInscriptionsForAnimation(Long animationId) {
        return inscriptionRepo.countByAnimationId(animationId);
    }
}
