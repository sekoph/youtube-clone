package org.sekoph.videoservice.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.sekoph.videoservice.dto.VideoRequestDTO;
import org.sekoph.videoservice.dto.VideoResponseDTO;
import org.sekoph.videoservice.exception.VideoProcessingException;
import org.sekoph.videoservice.mapper.VideoMapper;
import org.sekoph.videoservice.minIO.UploadToMiniO;
import org.sekoph.videoservice.model.Video;
import org.sekoph.videoservice.model.VideoFrame;
import org.sekoph.videoservice.model.VideoSegment;
import org.sekoph.videoservice.model.enums.SegmentStatus;
import org.sekoph.videoservice.model.enums.VideoStatus;
import org.sekoph.videoservice.repository.VideoRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VideoService implements DisposableBean {

    private final VideoRepository videoRepository;
    private final MinioClient minioClient;
    private final ExecutorService executorService;
    private final UploadToMiniO uploadToMiniO;

    @Value("${minio.bucket.videos}")
    private String videosBucket;

    @Value("${minio.bucket.segments}")
    private String segmentsBucket;

    @Value("${minio.bucket.frames}")
    private String framesBucket;

    @Value("${video.processing.segment-duration:300}")
    private int segmentDuration;

    @Value("${video.processing.frame-interval:10}")
    private int frameInterval;

    @Value("${video.processing.thread-pool-size:4}")
    private int threadPoolSize;

    @Autowired
    public VideoService(VideoRepository videoRepository, MinioClient minioClient, UploadToMiniO uploadToMiniO) {
        this.videoRepository = videoRepository;
        this.minioClient = minioClient;
        this.uploadToMiniO = uploadToMiniO;
        this.executorService = Executors.newFixedThreadPool(4); // Use default value during construction
    }

    @Override
    public void destroy() throws Exception {
        log.info("Shutting down video processing executor service");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("Executor service did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public VideoResponseDTO uploadVideo(VideoRequestDTO videoRequestDTO) {
        validateVideoRequest(videoRequestDTO);

        String originalFilename = videoRequestDTO.getFile().getOriginalFilename();
        log.info("Processing video upload for file: {}", originalFilename);
        String extension = FilenameUtils.getExtension(originalFilename);
        String s3Key = "video_" + UUID.randomUUID().toString() + "." + extension;

        // Upload the original file to Minio
        try{
            uploadToMiniO.uploadFile(videosBucket, s3Key, videoRequestDTO.getFile(), "video/" + extension);
        }catch (Exception e){
            log.error("Error while uploading video", e);
            throw new VideoProcessingException("Error while uploading video", e);
        }

        Video newVideo = createVideoEntity(videoRequestDTO, originalFilename, s3Key);
        Video savedVideo = videoRepository.save(newVideo);

        log.info("Video entity created with ID: {}", savedVideo.getId());
        executorService.execute(() -> processVideo(savedVideo));

        return VideoMapper.toDTO(savedVideo);
    }

    private void validateVideoRequest(VideoRequestDTO videoRequestDTO) {
        if (videoRequestDTO.getFile() == null || videoRequestDTO.getFile().isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }

        String filename = videoRequestDTO.getFile().getOriginalFilename();
        if (filename == null || !isValidVideoFormat(filename)) {
            throw new IllegalArgumentException("Invalid video file format");
        }
    }

    private boolean isValidVideoFormat(String filename) {
        String[] validExtensions = {".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv"};
        String lowerFilename = filename.toLowerCase();
        for (String ext : validExtensions) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private Video createVideoEntity(VideoRequestDTO videoRequestDTO, String originalFilename, String s3Key) {
        Video newVideo = new Video();
        newVideo.setOriginalFilename(originalFilename);
        newVideo.setVideoS3Key(s3Key);
        newVideo.setTitle(videoRequestDTO.getTitle());
        newVideo.setDescription(videoRequestDTO.getDescription());
        newVideo.setVisibility(videoRequestDTO.getVisibilityStatus());
        newVideo.setUserID(videoRequestDTO.getUserId());
        newVideo.setVideoStatus(VideoStatus.UPLOADED);
        newVideo.setViews(0);
        newVideo.setCreatedAt(LocalDateTime.now());
        newVideo.setUpdatedAt(LocalDateTime.now());
        return newVideo;
    }

    private void processVideo(Video savedVideo) {
        try {
            log.info("Starting video processing for video ID: {}", savedVideo.getId());
            updateVideoStatus(savedVideo, VideoStatus.PROCESSING);

            // Step 1: Extract video metadata
            extractVideoMetadata(savedVideo);
            log.info("Video metadata extracted for video ID: {}", savedVideo.getId());

            // Step 2: Segment the video
            segmentVideo(savedVideo);
            log.info("Video segmentation completed for video ID: {}", savedVideo.getId());

            // Step 3: Extract key frames
            extractKeyFrames(savedVideo);
            log.info("Key frame extraction completed for video ID: {}", savedVideo.getId());

            updateVideoStatus(savedVideo, VideoStatus.READY);
            log.info("Video processing completed successfully for video ID: {}", savedVideo.getId());

        } catch (Exception e) {
            log.error("Video processing failed for video ID: {}", savedVideo.getId(), e);
            updateVideoStatus(savedVideo, VideoStatus.FAILED);
            // Consider publishing an event or notification here
        }
    }

    private void updateVideoStatus(Video video, VideoStatus status) {
        video.setVideoStatus(status);
        video.setUpdatedAt(LocalDateTime.now());
        videoRepository.save(video);
    }

    private void extractVideoMetadata(Video savedVideo) throws Exception {
        log.debug("Extracting metadata for video: {}", savedVideo.getOriginalFilename());

        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                getVideoPath(savedVideo)
        );

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new VideoProcessingException("FFprobe failed with exit code: " + exitCode +
                    ". Error: " + errorOutput.toString());
        }

        try {
            double duration = Double.parseDouble(output.toString().trim());
            savedVideo.setDuration((long) duration);
            savedVideo.setUpdatedAt(LocalDateTime.now());
            videoRepository.save(savedVideo);
            log.debug("Video duration set to: {} seconds", duration);
        } catch (NumberFormatException e) {
            throw new VideoProcessingException("Failed to parse video duration: " + output.toString(), e);
        }
    }

    private void segmentVideo(Video savedVideo) throws Exception {
        log.debug("Starting video segmentation for video ID: {}", savedVideo.getId());

        List<VideoSegment> segments = new ArrayList<>();
        long videoDuration = savedVideo.getDuration();
        int segmentNumber = 0;

        for (long start = 0; start < videoDuration; start += segmentDuration) {
            long end = Math.min(start + segmentDuration, videoDuration);

            VideoSegment segment = createVideoSegment(savedVideo, start, end, segmentNumber);
            segments.add(segment);
            segmentNumber++;
        }

        savedVideo.setSegments(segments);
        videoRepository.save(savedVideo);
        log.debug("Created {} segments for video ID: {}", segments.size(), savedVideo.getId());
    }

    private VideoSegment createVideoSegment(Video savedVideo, long start, long end, int segmentNumber)
            throws Exception {

        String segmentID = UUID.randomUUID().toString();
        String segmentKey = "segment_" + segmentID + ".mp4";

        // Create temporary file for the segment
        File tempSegmentFile = File.createTempFile(segmentID, ".mp4");
        tempSegmentFile.deleteOnExit();

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", getVideoPath(savedVideo),
                "-ss", String.valueOf(start),
                "-t", String.valueOf(end - start),
                "-c", "copy",
                "-avoid_negative_ts", "make_zero",
                tempSegmentFile.getAbsolutePath()
        );

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new VideoProcessingException("FFmpeg segmentation failed for segment " + segmentNumber +
                    " with exit code: " + exitCode);
        }

        // Upload segment to minIO
        try (InputStream inputStream = new FileInputStream(tempSegmentFile)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(segmentsBucket)
                            .object(segmentKey)
                            .stream(inputStream, tempSegmentFile.length(), -1)
                            .contentType("video/mp4")
                            .build()
            );
        }

        VideoSegment videoSegment = new VideoSegment();
        videoSegment.setSegmentID(segmentID);
        videoSegment.setSegmentNumber(segmentNumber);
        videoSegment.setStatus(SegmentStatus.READY); // Updated to READY after successful processing
        videoSegment.setSegmentS3Key(segmentKey);
        videoSegment.setStartTime(start);
        videoSegment.setEndTime(end);
        videoSegment.setFileSize(tempSegmentFile.length());
        videoSegment.setCreatedAt(LocalDateTime.now());

        return videoSegment;
    }

    private void extractKeyFrames(Video savedVideo) throws Exception {
        log.debug("Starting key frame extraction for video ID: {}", savedVideo.getId());

        List<VideoFrame> keyFrames = new ArrayList<>();
        long videoDuration = savedVideo.getDuration();
        int frameNumber = 0;

        for (long timestamp = 0; timestamp < videoDuration; timestamp += frameInterval) {
            VideoFrame frame = createVideoFrame(savedVideo, timestamp, frameNumber);
            keyFrames.add(frame);
            frameNumber++;
        }

        savedVideo.setKeyFrames(keyFrames);
        videoRepository.save(savedVideo);
        log.debug("Extracted {} key frames for video ID: {}", keyFrames.size(), savedVideo.getId());
    }

    private VideoFrame createVideoFrame(Video savedVideo, long timestamp, int frameNumber) throws Exception {
        String frameID = UUID.randomUUID().toString();
        String frameKey = "frame_" + frameID + ".jpg";

        // Create temporary file for the frame
        File tempFrameFile = File.createTempFile(frameID, ".mp4");
        tempFrameFile.deleteOnExit();

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", getVideoPath(savedVideo),
                "-ss", String.valueOf(timestamp),
                "-vframes", "1",
                "-q:v", "2",
                tempFrameFile.getAbsolutePath()
        );

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new VideoProcessingException("FFmpeg frame extraction failed for frame " + frameNumber +
                    " at timestamp " + timestamp + " with exit code: " + exitCode);
        }

        // Upload to minIO

        try(InputStream inputStream = new FileInputStream(tempFrameFile)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(framesBucket)
                            .object(frameKey)
                            .stream(inputStream, tempFrameFile.length(), -1)
                            .contentType("image/jpeg")
                            .build()
            );
        }


        VideoFrame videoFrame = new VideoFrame();
        videoFrame.setFrameID(frameID);
        videoFrame.setFrameNumber((long) frameNumber); // Fixed: using frameNumber instead of fileSize
        videoFrame.setTimeStamp(timestamp);
        videoFrame.setIsKeyFrame(true);
        videoFrame.setFrameType("I-frame");
        videoFrame.setFrameS3Key(frameKey);
        videoFrame.setFileSize(tempFrameFile.length());
        videoFrame.setCreatedAt(LocalDateTime.now());

        return videoFrame;
    }

//    private long getObjectSize(String bucket, String objectKey) throws Exception {
//        return minioClient.statObject(
//                StatObjectArgs.builder()
//                        .bucket(bucket)
//                        .object(objectKey)
//                        .build()
//        ).size();
//    }

//    private String getFramePath(String frameKey) {
//        return "minio/" + framesBucket + "/" + frameKey;
//    }

    private String getVideoPath(Video savedVideo) {
        return "minio/" + videosBucket + "/" + savedVideo.getOriginalFilename();
    }

//    private String getSegmentPath(String segmentKey) {
//        return "minio/" + segmentsBucket + "/" + segmentKey;
//    }
}