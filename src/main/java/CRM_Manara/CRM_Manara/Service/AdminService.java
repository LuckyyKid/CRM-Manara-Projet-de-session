package CRM_Manara.CRM_Manara.Service;

import CRM_Manara.CRM_Manara.Repository.EnfantRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired
    private EnfantRepo enfantRepo;
    private ParentRepo parentRepo;



}
