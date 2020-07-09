package com.test.elk.todoservice;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

    @RestController
    @RequestMapping("todos")
    @RequiredArgsConstructor
    @Slf4j
    public static class Controller {

        private final WebClient webClient;
        private final ParameterizedTypeReference<List<Todo>> typeStringList = new ParameterizedTypeReference<List<Todo>>() {
        };

        @GetMapping
        public List<Todo> getTodos() {
            log.info("Getting todos");
            List<Todo> body = webClient.get().uri("todos")
                    .retrieve().bodyToMono(typeStringList)
                    .blockOptional()
                    .orElse(Collections.emptyList());
            log.info("Got response of {} todos", body.size());
            return body;
        }

        @GetMapping(path = "{id}")
        public Todo getTodo(@PathVariable String id) {
            log.info("Getting todo for id:{}", id);
            Todo body = webClient.get().uri("todos/{id}", id)
                    .retrieve().bodyToMono(Todo.class)
                    .blockOptional()
                    .orElseThrow(IllegalArgumentException::new);
            log.info("Got response for todo: {}", body);
            return body;
        }

    }

    @Value("${service.mock.base-url}")
    String baseUrl;

    @Bean
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(baseUrl).filter(logRequest()).build();
    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info("Request Header {}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }

    @Data
    public static class Todo {
        private int id;
        private boolean completed;
        private String title;
        private int userId;
    }
}
