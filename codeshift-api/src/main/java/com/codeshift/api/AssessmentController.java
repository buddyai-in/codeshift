package com.codeshift.api;

import com.codeshift.assessment.AssessmentResult;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Public (no-auth) free assessment — the top-of-funnel funnel entry (product doc
 * §21.2). Upload a codebase, get an assessment report + price estimate, no account.
 */
@RestController
@RequestMapping("/public/assess")
public class AssessmentController {

    private final AssessmentService assessment;

    public AssessmentController(AssessmentService assessment) {
        this.assessment = assessment;
    }

    /** Upload a source zip → assessment report. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssessmentResult assessUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectName", required = false) String projectName) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty upload");
        }
        String name = projectName != null ? projectName : stripExt(file.getOriginalFilename());
        try {
            return assessment.assessZip(name, file.getInputStream());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read zip: "
                    + e.getMessage());
        }
    }

    /** Assess a server-accessible source directory (handy for demos / CI). */
    @PostMapping(path = "/path", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssessmentResult assessPath(@RequestBody PathRequest req) {
        return assessment.assessDirectory(req.projectName(), Path.of(req.projectPath()));
    }

    public record PathRequest(String projectPath, String projectName) {}

    private static String stripExt(String filename) {
        if (filename == null) {
            return "uploaded-project";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
