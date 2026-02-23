package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;

import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service

public class parentService {
    @Autowired
    private ParentRepo parentRepo;
    @Autowired
    private UserRepo userRepo;

    public void signUp(Parent parent, String email, String password) {
        // 1. Créer le User avec le mot de passe encodé
        User user = new User(email, password, SecurityRole.ROLE_PARENT);
        userRepo.save(user);

        // 2. Lier le User au Parent et sauvegarder
        parent.setUser(user);
        parentRepo.save(parent);
    }



}
