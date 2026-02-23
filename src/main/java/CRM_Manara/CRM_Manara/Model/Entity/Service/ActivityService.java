package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Repository.ActivityRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ActivityService {

    @Autowired
    ActivityRepo activityRepo;

    @Autowired
    AnimationRepo animationRepo;

    @Transactional(readOnly = true)
    public List<Activity> getAllActivities() {
        return activityRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Activity getActivityById(Long id) {
        return activityRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable"));
    }

    @Transactional(readOnly = true)
    public List<Animation> getAnimationsForActivity(Long activityId) {
        return animationRepo.findByActivityId(activityId);
    }
}
