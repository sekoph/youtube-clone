package org.sekoph.videoservice.repository;


import org.sekoph.videoservice.model.Video;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoRepository extends MongoRepository<Video, String> {
}
