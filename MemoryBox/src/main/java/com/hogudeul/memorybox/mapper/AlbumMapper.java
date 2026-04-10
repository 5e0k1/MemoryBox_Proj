package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.AlbumOption;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumMapper {

    List<AlbumOption> findActiveAlbumsByUserId(@Param("userId") Long userId);
}
