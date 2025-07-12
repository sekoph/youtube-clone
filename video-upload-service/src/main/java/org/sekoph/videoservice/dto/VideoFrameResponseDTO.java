package org.sekoph.videoservice.dto;


import lombok.Data;

@Data
public class VideoFrameResponseDTO {
    private String frameID;
    private Long frameNumber;
    private Long timestamp;
    private String frameUrl;
    private Integer width;
    private Integer height;
}
