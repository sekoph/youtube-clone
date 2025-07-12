package org.sekoph.videoservice.mapper;

import org.sekoph.videoservice.dto.VideoResponseDTO;
import org.sekoph.videoservice.model.Video;

public class VideoMapper {
    public static VideoResponseDTO toDTO(Video video) {
        VideoResponseDTO videoResponseDTO = new VideoResponseDTO();
        videoResponseDTO.setId(video.getId());
        videoResponseDTO.setTitle(video.getTitle());
        videoResponseDTO.setDescription(video.getDescription());
        videoResponseDTO.setUserID(video.getUserID());
        videoResponseDTO.setVideoStatus(video.getVideoStatus());
        videoResponseDTO.setVisibility(video.getVisibility());
        videoResponseDTO.setViews(video.getViews());
        videoResponseDTO.setDuration(video.getDuration());
        videoResponseDTO.setCreatedAt(video.getCreatedAt());
        videoResponseDTO.setUpdatedAt(video.getUpdatedAt());

        return videoResponseDTO;
    }
}
