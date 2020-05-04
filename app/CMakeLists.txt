cmake_minimum_required(VERSION 3.4.1)


add_library( audio_lib
             SHARED
             src/main/cpp/audio_lib.cpp
        )
add_library( vedio_lib
             SHARED
             src/main/cpp/vedio_lib.cpp
             )

include_directories(libs/include)
#设置so的路径
set(ffmpeg_DIR ${CMAKE_SOURCE_DIR}/libs)
add_library( avcodec-57
             SHARED
             IMPORTED)
set_target_properties( avcodec-57
                       PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_DIR}/armeabi-v7a/libavcodec-57.so)


add_library( avfilter-6
             SHARED
             IMPORTED)
set_target_properties( avfilter-6
                       PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_DIR}/armeabi-v7a/libavfilter-6.so)

add_library( avformat-57
             SHARED
             IMPORTED)
set_target_properties( avformat-57
                       PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_DIR}/armeabi-v7a/libavformat-57.so)

add_library( avutil-55
             SHARED
             IMPORTED)
set_target_properties( avutil-55
                       PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_DIR}/armeabi-v7a/libavutil-55.so)

add_library( avdevice-57
             SHARED
             IMPORTED)
set_target_properties( avdevice-57
                       PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_DIR}/armeabi-v7a/libavdevice-57.so)

add_library( swresample-2
             SHARED
             IMPORTED)
set_target_properties( swresample-2
                       PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_DIR}/armeabi-v7a/libswresample-2.so)

add_library( swscale-4
             SHARED
             IMPORTED)
set_target_properties( swscale-4
                       PROPERTIES IMPORTED_LOCATION
                       ${ffmpeg_DIR}/armeabi-v7a/libswscale-4.so)




find_library(
              log-lib
              log )

target_link_libraries(
                       audio_lib
                       vedio_lib
                       avfilter-6  #滤镜特效处理
                       avformat-57 #封装格式处理
                       avcodec-57  #编解码
                       avdevice-57 #设备信息
                       avutil-55   #工具类
                       swresample-2#音频采样数据格式转换库
                       swscale-4   #视频像素数据格式转换
                       ${log-lib} )