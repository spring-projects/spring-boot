package org.springframework.boot.autoconfigure.web.reactive;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.util.unit.DataSize;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MultipartProperties.class)
class DefaultPartHttpMessageReaderCustomizerConfiguration {

	@Bean
	@Order(-10)
	CodecCustomizer defaultPartHttpMessageReaderCustomizer(MultipartProperties multipartProperties) {
		return (configurer) -> configurer.defaultCodecs().configureDefaultCodec((codec) -> {
			if (!DefaultPartHttpMessageReader.class.isInstance(codec)) {
				return;
			}
			DefaultPartHttpMessageReader defaultPartHttpMessageReader = (DefaultPartHttpMessageReader) codec;
			boolean streaming = multipartProperties.getStreaming() != null && multipartProperties.getStreaming();
			boolean unlimitedMaxMemorySize = multipartProperties.getMaxInMemorySize() != null
					&& multipartProperties.getMaxInMemorySize().toBytes() == -1;
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(multipartProperties::getStreaming).to(defaultPartHttpMessageReader::setStreaming);
			if (!streaming) {
				map.from(multipartProperties::getMaxInMemorySize).asInt(this::convertToBytes)
						.to(defaultPartHttpMessageReader::setMaxInMemorySize);
			}
			if (!streaming && !unlimitedMaxMemorySize) {
				map.from(multipartProperties::getMaxDiskUsagePerPart).as(this::convertToBytes)
						.to(defaultPartHttpMessageReader::setMaxDiskUsagePerPart);
				map.from(multipartProperties::getFileStorageDirectory)
						.to(dir -> setFileStorageDirectory(dir, defaultPartHttpMessageReader));
			}
			map.from(multipartProperties::getMaxParts).to(defaultPartHttpMessageReader::setMaxParts);
			map.from(multipartProperties::getMaxHeadersSize).asInt(this::convertToBytes)
					.to(defaultPartHttpMessageReader::setMaxHeadersSize);
			map.from(multipartProperties::getHeadersCharset).to(defaultPartHttpMessageReader::setHeadersCharset);
		});
	}

	private void setFileStorageDirectory(String fileStorageDirectory,
			DefaultPartHttpMessageReader defaultPartHttpMessageReader) {
		try {
			defaultPartHttpMessageReader.setFileStorageDirectory(Paths.get(fileStorageDirectory));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private long convertToBytes(DataSize size) {
		return (size != null) ? size.toBytes() : null;
	}

}
