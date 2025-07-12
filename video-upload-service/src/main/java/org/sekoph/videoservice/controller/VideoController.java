package org.sekoph.videoservice.controller;


import org.sekoph.videoservice.dto.VideoRequestDTO;
import org.sekoph.videoservice.dto.VideoResponseDTO;
import org.sekoph.videoservice.service.VideoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video")
public class VideoController {
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoResponseDTO> createVideo(@ModelAttribute VideoRequestDTO videoRequestDTO) {
        return ResponseEntity.ok().body(videoService.uploadVideo(videoRequestDTO));
    }
}
