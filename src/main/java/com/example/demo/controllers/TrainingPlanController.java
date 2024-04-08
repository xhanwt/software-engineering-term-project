package com.example.demo.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.models.TrainingPlan;
import com.example.demo.models.User;
import com.example.demo.models.UserRepository;
import com.example.demo.service.Validation;
import com.example.demo.service.TrainingPlanService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class TrainingPlanController {

    @Autowired
    private UserRepository userRepo;


    @GetMapping("/trainingPlan/viewAll")
    public String viewTrainingPlan(@RequestParam Map<String, String> newUser, HttpServletResponse response,
            Model model, HttpSession session) {
        // check if user is logged in
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        Integer userId = (int) session.getAttribute("userId");
        User user = userRepo.findByUid(userId);
        model.addAttribute("user", user);
        List<TrainingPlan> trainingPlans = user.getTrainingPlans();
        model.addAttribute("trainingPlans", trainingPlans);

        if (!trainingPlans.isEmpty()) {
            // Accessing the first training plan if the list is not empty
            System.out.println(trainingPlans.get(0).getName() + " plan :");
            System.out.println(trainingPlans.get(0).getTrainingSessions());
        }
        return "training_plans/viewAllTrainingPlan";
    }

    @GetMapping("/trainingPlan/add")
    public String trainingPlanTest(HttpSession session, HttpServletResponse response, Model model) {
        try {
            // check if user is logged in
            if (session.getAttribute("userId") == null) {
                return "redirect:/login";
            }
            int userId = (int) session.getAttribute("userId");
            User user = userRepo.findByUid(userId);
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("userId", userId);
                return "training_plans/addTrainingPlan";
            } else {
                // Handling case where user does not exist
                response.setStatus(404); // Not Found
                model.addAttribute("error", "User does not exist");
                return "training_plans/addTrainingPlan";
            }
        } catch (NumberFormatException e) {
            // Handling case where userId is not a valid integer
            response.setStatus(400); // Bad Request
            model.addAttribute("error", "Invalid User ID");
            return "training_plans/addTrainingPlan";
        }
    }

    @PostMapping("/trainingPlan/add/submit")
    public String addPlan(@RequestParam Map<String, String> newPlan,
            @RequestParam(value = "trainingSessions", required = false) String[] selectedSessions, HttpSession session,
            HttpServletResponse response, Model model) {

        // check if user is logged in
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        int userId = (int) session.getAttribute("userId");
        String newName = newPlan.get("name").strip();
        String newDesc = newPlan.get("description").strip();
        String startDateStr = newPlan.get("sdate");
        String endDateStr = newPlan.get("edate");

        Validation validate = TrainingPlanService.validateTrainingPlan(userRepo, userId, newName, newDesc, startDateStr,
                endDateStr);

        // Check if name and description are present
        if (validate.isError) {
            response.setStatus(validate.status); // Bad Request
            model.addAttribute("error", validate.message);
            model.addAttribute("userId", userId);
            model.addAttribute("name", newName);
            model.addAttribute("description", newDesc);
            model.addAttribute("sDate", startDateStr);
            model.addAttribute("eDate", endDateStr);
            return "training_plans/addTrainingPlan";
        }

        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        TrainingPlan newTrainingPlan = new TrainingPlan(newName, newDesc, startDate, endDate);
        User user = userRepo.findByUid(userId);

        // System.out.println("Training Session Names:");
        /*
         * for (int i = 0; i < ts.size(); i++) {
         * System.out.println(ts.get(i).getName());
         * }
         * System.out.println("NEW TRAINING PLAN TEST:");
         * for (int i = 0; i < newTrainingPlan.getTrainingSessions().size(); i++) {
         * System.out.println(newTrainingPlan.getTrainingSessions().get(i).getName());
         * }
         */

        if (user != null) {
            user.addTrainingPlan(newTrainingPlan);
            userRepo.save(user); // Save the user with the new training plan
            System.out.println("Successfully Added");
        } else {
            System.out.println("Attempted to add training plan to a null User.");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/trainingPlan/delete")
    public String deleteTrainingPlan(@RequestParam Map<String, String> deleteForm, Model model, HttpSession session) {
        // check if user is logged in
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        int userId = (int) session.getAttribute("userId");
        int tpid = Integer.parseInt(deleteForm.get("tpid"));

        User user = userRepo.findByUid(userId);
        if (user != null) {
            // Find the training plan by ID
            TrainingPlan planToDelete = user.getTrainingPlans().stream()
                    .filter(plan -> plan.getTpid() == tpid)
                    .findFirst()
                    .orElse(null);

            if (planToDelete != null) {
                user.removeTrainingPlan(planToDelete);
                userRepo.save(user); // Save the user to update the training plan list
                model.addAttribute("success", "Training plan deleted successfully");
            } else {
                model.addAttribute("error", "Training plan not found");
            }
        } else {
            model.addAttribute("error", "User not found");
        }
        return "redirect:/trainingPlan/viewAll";
    }

}
