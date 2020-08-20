
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.ffmpeg.*;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avutil.*;
import org.bytedeco.ffmpeg.avfilter.*;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.PointerPointer;

import java.io.IOException;

import static org.bytedeco.ffmpeg.global.avformat.AVFMT_NOFILE;
import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;

public class antmed {
    static StreamContext[] streamContexts;
    static AVFormatContext output_format_context, input_format_context;
    public static void main(String[] args) {


        input_format_context = new AVFormatContext(null);
        output_format_context = new AVFormatContext(null);

        //Dosyayı açıyorum ve stream infoyu alıyorum
        avformat.avformat_open_input((input_format_context), "/home/taso/Desktop/java/C.flv", null, null);
        avformat.avformat_find_stream_info((input_format_context), (PointerPointer) null);

        streamContexts = new StreamContext[input_format_context.nb_streams()];
        for (int i = 0; i < input_format_context.nb_streams(); i++) {
            streamContexts[i] = new StreamContext();
            //Input streamlerin decoderlarını buluyorum
            AVStream stream = input_format_context.streams(i);
            AVCodec decoder = avcodec.avcodec_find_decoder(stream.codecpar().codec_id());

            //Decoderın ne olduğunu tutan context yaratıyorum
            AVCodecContext codecContext = avcodec.avcodec_alloc_context3(decoder);
            streamContexts[i].decoderContext = codecContext;
        }
        openOutput("/home/taso/Desktop/java/outpuuuuut.mp4");

        System.out.println(streamContexts[0].decoderContext.codec().name().getString());

        //packet frameleri tutacak
        AVPacket packet = new AVPacket();

        //Streamleri framelerle kopyalıyorum
        while (avformat.av_read_frame(input_format_context, packet) >= 0) {
            int streamIndex = packet.stream_index();
            //System.out.println(streamIndex);

            //packetleri kopyalayan method
            avcodec.av_packet_rescale_ts(packet, input_format_context.streams(streamIndex).time_base(),
                    output_format_context.streams(streamIndex).time_base());
            avformat.av_interleaved_write_frame(output_format_context, packet);
            avcodec.av_packet_unref(packet);
        }
        //Streamleri dosyaya yazdır
        avformat.av_write_trailer(output_format_context);
    }
    static class StreamContext {
        AVCodecContext decoderContext;
    }

    //Output initialize methodu
    static AVFormatContext openOutput(String fileName) {
        output_format_context= new AVFormatContext(null);
        avformat.avformat_alloc_output_context2(output_format_context, null, null, fileName);

        for (int i = 0; i < input_format_context.nb_streams(); i++) {
            AVCodec c = new AVCodec(null);
            AVStream outStream = avformat.avformat_new_stream(output_format_context, c);
            AVStream inStream = input_format_context.streams(i);

            //Remux ediyoruz, transcode etmeden kopyalayıp containerı değiştiriyoruz
            avcodec.avcodec_parameters_copy(outStream.codecpar(), inStream.codecpar());
            outStream.time_base(inStream.time_base());
        }
        //Print ettiyorum format informationını
        avformat.av_dump_format(output_format_context, 0, fileName, 1);
        if ((output_format_context.flags() & AVFMT_NOFILE) != AVFMT_NOFILE) {
            //Bytestream IO context
            AVIOContext c = new AVIOContext();
            avformat.avio_open(c, fileName, AVIO_FLAG_WRITE);
            output_format_context.pb(c);
        }
        //Dosyanın içine istediğim contexti yazıyor
        avformat.avformat_write_header(output_format_context, (AVDictionary) null);
        return output_format_context;
    }

}
