package com.clock.webhook.processor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.clock.webhook.model.Webhook;
import com.clock.webhook.service.ClockService;
import com.clock.webhook.utils.ClockUtils;

@Service
public class WebhookProcessor implements Runnable {

	@Autowired
	private ClockService clockService;

	private ScheduledExecutorService schedulerExecutor;

	private HttpClient client;

	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	@PostConstruct
	public void init() {
		client = HttpClient.newBuilder().build();
		schedulerExecutor = Executors.newSingleThreadScheduledExecutor();
	}

	@Override
	public void run() {

		while (!clockService.getQueue().isEmpty()) {

			Webhook webhook = clockService.getQueue().poll();

			TimerTask repeatedTask = new TimerTask() {

				@Override
				public void run() {

					try {

						HttpRequest request = HttpRequest.newBuilder().uri(URI.create(webhook.getUrl()))
								.POST(BodyPublishers.ofString(LocalDateTime.now().format(formatter))).build();

						client.send(request, BodyHandlers.discarding());

					} catch (IOException | InterruptedException e) {
						// TODO Exception handling
						e.printStackTrace();
					}
				}
			};

			TimeUnit currentUnit = ClockUtils.getTimeUnitBy(webhook.getUnit());

			ScheduledFuture<?> future = schedulerExecutor.scheduleAtFixedRate(repeatedTask, 0,
					Long.valueOf(webhook.getInterval()), currentUnit);

			clockService.getWebhooks().put(webhook, future);

		}
	}

}