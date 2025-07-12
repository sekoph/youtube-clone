package org.sekoph.videoservice.dto;

import lombok.Data;
import org.sekoph.videoservice.model.enums.VideoStatus;
import org.sekoph.videoservice.model.enums.VisibleStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class VideoResponseDTO {
    private String id;
    private String title;
    private String description;
    private UUID userID;
    private VideoStatus videoStatus;
    private String videoS3Key;
    private VisibleStatus visibility;
    private Integer views;
    private Long duration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Response DTO for segments and frames
    private List<VideoSegmentResponseDto> segments;
    private List<VideoFrameResponseDTO> keyFrames;


}
