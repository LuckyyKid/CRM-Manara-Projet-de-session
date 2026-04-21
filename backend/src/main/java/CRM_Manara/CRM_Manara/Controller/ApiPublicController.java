package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.ActivityDto;
import CRM_Manara.CRM_Manara.dto.ActivityRecommendationRequestDto;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.service.AdminService;
import CRM_Manara.CRM_Manara.service.ActivityRecommendationService;
import java.util.Comparator;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class ApiPublicController {

    private final AdminService adminService;
    private final ApiDtoMapper apiDtoMapper;
    private final ActivityRecommendationService activityRecommendationService;

    public ApiPublicController(
            AdminService adminService,
            ApiDtoMapper apiDtoMapper,
            ActivityRecommendationService activityRecommendationService
    ) {
        this.adminService = adminService;
        this.apiDtoMapper = apiDtoMapper;
        this.activityRecommendationService = activityRecommendationService;
    }

    @GetMapping("/activities")
    @Transactional(readOnly = true)
    public List<ActivityDto> activities() {
        return adminService.getAllActivities().stream()
                .sorted(Comparator.comparing(activity -> activity.getActivyName(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(apiDtoMapper::toActivityDto)
                .toList();
    }

    @PostMapping("/activity-recommendations")
    @Transactional(readOnly = true)
    public ActivityRecommendationService.RecommendationResponse activityRecommendations(
            @RequestBody ActivityRecommendationRequestDto request
    ) {
        return activityRecommendationService.recommend(request.age(), request.profile(), request.goal());
    }
}
