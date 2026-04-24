package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.AlbumOption;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlbumMapper {

    List<AlbumOption> findActiveAlbums();

    Long selectNextAlbumId();

    Integer selectNextSortOrder();

    AlbumOption findActiveAlbumByName(@Param("albumName") String albumName);

    int insertAlbum(@Param("albumId") Long albumId,
                    @Param("albumName") String albumName,
                    @Param("sortOrder") Integer sortOrder);
}
