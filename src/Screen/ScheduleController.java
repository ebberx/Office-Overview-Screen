package Screen;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.converter.DurationConverter;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Date;
import java.time.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Properties;

public class ScheduleController extends Application {

    StackPane scheduleRoot;
    Scene scene;
    Stage stage;
    Canvas canvas;
    ArrayList<Consultant> consultants = new ArrayList<Consultant>();
    GraphicsContext gc;

    String officeName;
    int displayScreen;

    @Override
    public void start(Stage stage) throws Exception {

        // Load office name from configuration file
        {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream("conf.properties"));
                officeName = props.getProperty("office", "Main office");
                displayScreen = Integer.parseInt(props.getProperty("screen", "1"));

                // -== Validation of configuration values ==-
                // Is screen is valid? Primary if not
                if(Screen.getScreens().get(displayScreen) == null)
                    displayScreen = 0;
            }
            catch (Exception e) { e.printStackTrace(); }
        }

        this.stage = stage; // save stage for later use
        stage.setTitle("Pomodoro Overview");
        stage.setFullScreen(true);

        // Setup UI layout
        scheduleRoot = new StackPane();
        //scheduleRoot.setStyle("-fx-background-color: ");
        scene = new Scene(scheduleRoot);

        // Canvas the size of the screen

        double width = Screen.getScreens().get(displayScreen).getBounds().getWidth();
        double height = Screen.getScreens().get(displayScreen).getBounds().getHeight();
        System.out.println("[Screen] Width: " + width + " Height: " + height);
        canvas = new Canvas(width, height);
        canvas.getGraphicsContext2D().setFontSmoothingType(FontSmoothingType.LCD);

        // Add to stage/scene
        scheduleRoot.getChildren().add(canvas);
        stage.setScene(scene);
        stage.setX(Screen.getScreens().get(displayScreen).getBounds().getMinX());
        stage.setY(Screen.getScreens().get(displayScreen).getBounds().getMinY());
        stage.show();

        // Exit properly
        stage.setOnCloseRequest(t -> {
            exit();
        });

        // Load initial data
        DB.getInstance().connect();
        consultants = DB.getInstance().getConsultantsInOffice(officeName);
        for(Consultant c : consultants) {
            DB.getInstance().getWorkdaysOfConsultant(c, LocalDate.now().toString()+" 00:00:00");

            if(c.getWorkdays() != null) {
                for(Workday w : c.getWorkdays()) {
                    DB.getInstance().getPomodorosInWorkday(w);
                }
            }
        }
        DB.getInstance().disconnect();

        // Start render update with frequency of 1 sec
        Timeline tl = new Timeline(new KeyFrame(javafx.util.Duration.millis(1000), (e) ->{
            System.out.printf("Render time: %d microseconds\n", render() / 1000);
        }));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }

    int schedulePaddingTop = 100;
    public long render() {
        long start = System.nanoTime();

        if(gc == null)
            gc = canvas.getGraphicsContext2D();

        // Clear canvas before drawing
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Background
        gc.setFill(Color.web("#EEF5FD"));
        gc.fillRect(0,0,canvas.getWidth(), canvas.getHeight());

        // Draw time
        gc.setFill(Color.BLACK);
        Font font = new Font("Segoe UI", 38);
        gc.setFont(font);
        gc.fillText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), canvas.getWidth() / 2 - 50, 46);

        font = new Font("Segoe UI", 18);
        gc.setFont(font);

        int i = 0;
        if(consultants != null) {
            for(Consultant c : consultants) {
                gc.setFill(Color.BLACK);
                gc.fillRect(0, schedulePaddingTop + 18 + (100*i), canvas.getWidth(), 54);
                gc.setFill(Color.WHITE);
                gc.fillRect(0, schedulePaddingTop + 20 + (100*i), canvas.getWidth(), 50);

                // Draw schedule
                if(c.getWorkdays() != null) {
                    for(Workday w : c.getWorkdays()) {
                        for(Pomodoro p : w.pomodoros) {
                            gc.setFill(Color.web("FF6962"));
                            gc.fillRect(timeToCanvasX(p.start), schedulePaddingTop + 20 + (100*i), pixelsInTimespan(p.workDuration), 50);

                            gc.setFill(Color.web("77DD76"));
                            double breakX = timeToCanvasX(p.start) + pixelsInTimespan(p.workDuration);
                            gc.fillRect(breakX, schedulePaddingTop + 20 + (100*i), pixelsInTimespan(p.breakDuration), 50);
                        }
                    }
                }

                // Draw name
                gc.setFill(Color.BLACK);
                gc.fillText(c.getName(), 10, schedulePaddingTop + 12 + (100*i));
                i++;
            }

            // Draw timeline
            double timelineY = canvas.getHeight() - (canvas.getHeight() / 20);
            double timelineH = canvas.getHeight() / 20;
            gc.setFill(Color.BLACK);
            gc.fillRect(0, timelineY, canvas.getWidth(), timelineH);
            //gc.setFill(Color.web("#EEF5FD"));
            gc.setFill(Color.WHITE);
            gc.fillRect(4, timelineY+4, canvas.getWidth()-8, timelineH-8);

            gc.setFill(Color.BLACK);
            font = new Font("Segoe UI", 12);
            gc.setFont(font);

            gc.setLineWidth(1);
            double l = timelineH/8;
            gc.setLineDashes( l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l,l);
            gc.strokeLine(0,timelineY+timelineH/2, canvas.getWidth(), timelineY+timelineH/2);
            gc.setLineWidth(2);
            gc.setLineDashes(0);
            for(int j = -3; j < 8; j++){
                LocalTime hour = LocalTime.of(LocalTime.now().getHour(), 0);
                LocalDateTime date = LocalDateTime.of(LocalDate.now(), hour);

                gc.strokeLine(timeToCanvasX(date.plusHours(j)),
                        timelineY+2, timeToCanvasX(date.plusHours(j)), timelineY+timelineH);
                gc.setFill(Color.WHITE);
                gc.fillRect(timeToCanvasX(date.plusHours(j))-19, timelineY + timelineH / 2 -10, 40, 20);
                gc.setFill(Color.BLACK);
                gc.fillText(hour.plusHours(j).format(DateTimeFormatter.ofPattern("HH:mm")),
                        timeToCanvasX(date.plusHours(j))-14, timelineY + timelineH / 2 + 4);
            }
        }

        // Draw time slider
        double sliderX = canvas.getWidth() / 8 * 2;
        double sliderY = 0;
        double sliderW = 4;
        double sliderH = canvas.getHeight();
        gc.setFill(Color.BLACK);
        gc.fillRect(sliderX, sliderY, sliderW, sliderH);

        return System.nanoTime() - start;
    }

    public double timeToCanvasX(LocalDateTime time) {
        double currentTime = canvas.getWidth() / 8 * 2;
        //    Canvas timespan: canvas width divided by 8
        double pixelsPerHour = canvas.getWidth() / 8;
        java.time.Duration delta = java.time.Duration.between(LocalDateTime.now(), time);
        return (((double)delta.toSeconds() / 3600) * pixelsPerHour) + currentTime;
    }

    public double pixelsInTimespan(java.time.Duration time) {
        double pixelsPerHour = canvas.getWidth() / 8;
        return ((double)time.toSeconds() / 3600) * pixelsPerHour;
    }

    public void exit() {
        Platform.setImplicitExit(true);
        Platform.exit();
        System.exit(0);
    }
}
