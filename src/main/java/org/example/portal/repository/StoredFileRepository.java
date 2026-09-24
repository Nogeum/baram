package org.example.portal.repository;
import org.example.portal.domain.StoredFile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface StoredFileRepository extends JpaRepository<StoredFile,Long> {
interface FileInfo {Long getId();String getFilename();long getFileSize();java.time.LocalDateTime getCreatedAt();}
@Query("select f.id as id,f.filename as filename,f.fileSize as fileSize,f.createdAt as createdAt from StoredFile f where f.ownerType=:type and f.ownerId=:owner order by f.createdAt desc")
List<FileInfo> metadata(@Param("type") String type,@Param("owner") Long owner);
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from StoredFile r where r.id=:id")
Optional<StoredFile> lockById(@Param("id") Long id);
}
