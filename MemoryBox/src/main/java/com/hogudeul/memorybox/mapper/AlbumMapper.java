package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.AlbumOption;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlbumMapper {

    List<AlbumOption> findActiveAlbums();
}
