package org.sekoph.videoservice.dto;

import lombok.Data;

@Data
public class VideoSegmentResponseDto {
    private String segmentId;
    private String segmentNumber;
    private Long startTime;
    private Long endTime;
    private String segmentURL;
}
