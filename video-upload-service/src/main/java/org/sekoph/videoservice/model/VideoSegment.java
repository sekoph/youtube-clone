package org.sekoph.videoservice.model;

import lombok.Data;
import org.sekoph.videoservice.model.enums.SegmentStatus;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
public class VideoSegment {
    @Field("segment_id")
    private String segmentID;
    @Field("segment_number")
    private Integer segmentNumber;
    private SegmentStatus status;
    @Field("segment_s3_key")
    private String segmentS3Key;
    @Field("start_time")
    private Long startTime;
    @Field("end_time")
    private Long endTime;
    @Field("file_size")
    private Long fileSize;
    private String quality;

    @Field("created_time")
    private LocalDateTime createdAt;
}
