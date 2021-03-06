package com.diving.pungdong.service;

import com.diving.pungdong.config.S3Uploader;
import com.diving.pungdong.domain.lecture.Lecture;
import com.diving.pungdong.domain.lecture.LectureImage;
import com.diving.pungdong.dto.lecture.update.LectureImageUpdate;
import com.diving.pungdong.dto.lecture.update.LectureUpdateInfo;
import com.diving.pungdong.repo.LectureImageJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureImageService {
    private final LectureImageJpaRepo lectureImageJpaRepo;
    private final S3Uploader s3Uploader;

    public LectureImage saveLectureImage(LectureImage lectureImage) {
        return lectureImageJpaRepo.save(lectureImage);
    }

    public void deleteByURL(String lectureImageURL) {
        lectureImageJpaRepo.deleteByFileURI(lectureImageURL);
    }

    public void deleteIfIsDeleted(LectureUpdateInfo lectureUpdateInfo) {
        if (!lectureUpdateInfo.getLectureImageUpdateList().isEmpty()) {
            for (LectureImageUpdate lectureImageUpdate : lectureUpdateInfo.getLectureImageUpdateList()) {
                if (lectureImageUpdate.getIsDeleted()) {
                    lectureImageJpaRepo.deleteByFileURI(lectureImageUpdate.getLectureImageURL());
                    s3Uploader.deleteFileFromS3(lectureImageUpdate.getLectureImageURL());
                }
            }
        }
    }

    public void addList(String email, List<MultipartFile> addLectureImageFiles, Lecture lecture) throws IOException {
        if (!addLectureImageFiles.isEmpty()) {
            for (MultipartFile file : addLectureImageFiles) {
                String fileURI = s3Uploader.upload(file, "lecture", email);
                LectureImage lectureImage = LectureImage.builder()
                        .fileURI(fileURI)
                        .lecture(lecture)
                        .build();
                lectureImageJpaRepo.save(lectureImage);
            }
        }
    }
}
