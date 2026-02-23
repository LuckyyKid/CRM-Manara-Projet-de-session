package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;

import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service

public class parentService {

    @Autowired
    ParentRepo parentRepo;

    @Autowired
    UserRepo userRepo;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Transactional
    public void createNewParent( String email, String password,String nom , String prenom , String adresse)
    {
       String hash = passwordEncoder.encode(password);

        User user1 = new User(email,hash);

        user1.setRole(SecurityRole.ROLE_PARENT);

        User userSaved = userRepo.save(user1);


    }





}
