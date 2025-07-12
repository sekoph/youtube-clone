package org.sekoph.videoservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.sekoph.videoservice.model.enums.VisibleStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
public class VideoRequestDTO {
    @NotNull(message = "file cant be blank")
    private MultipartFile file;

    @NotBlank(message = "title cant be blank")
    private String title;

    private String description;

    @NotBlank(message = "user id cant be blank")
    private UUID userId;

    @NotBlank(message = "visibility status cant be blank")
    private VisibleStatus visibilityStatus;
}
