package projetarm_v2.simulator.ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.dockfx.DockPane;
import org.dockfx.DockPos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import projetarm_v2.simulator.boilerplate.ArmSimulator;

public class Gui extends Application {

	private Scene scene;

	private ArrayList<RegistersView> registersViews;
	private ArrayList<RamView> ramViews;
	private CodeEditor codeEditor;
	private ConsoleView consoleView;

	private ArmMenuBar armMenuBar;
	private ArmToolBar armToolBar;

	private DockPane dockPane;

	private File currentProgramPath;
	private boolean executionMode;

	private ArmSimulator simulator;
	private AtomicBoolean running;

	private Stage stage;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		this.simulator = new ArmSimulator();
		this.executionMode = false;
		this.running = new AtomicBoolean(false);
		this.stage = primaryStage;

		primaryStage.setTitle("#@RMStrong");
		Image applicationIcon = new Image("file:logo.png");
		primaryStage.getIcons().add(applicationIcon);

		this.dockPane = new DockPane();

		// load an image to caption the dock nodes
		// Image dockImage = new
		// Image(Gui.class.getResource("docknode.png").toExternalForm());

		// creating the nodes
		this.registersViews = new ArrayList<>();
		this.ramViews = new ArrayList<>();

		this.codeEditor = new CodeEditor();
		this.codeEditor.getNode().dock(dockPane, DockPos.LEFT);

		this.registersViews.add(new RegistersView(this.simulator));
		this.registersViews.get(0).getNode().dock(dockPane, DockPos.LEFT);

		this.ramViews.add(new RamView(simulator));
		this.ramViews.get(0).getNode().dock(dockPane, DockPos.RIGHT);

		this.consoleView = new ConsoleView();
		this.consoleView.getNode().dock(dockPane, DockPos.BOTTOM);

		this.armMenuBar = new ArmMenuBar(simulator, codeEditor, primaryStage, this.getHostServices());
		this.armToolBar = new ArmToolBar(simulator, codeEditor);

		VBox vbox = new VBox();
		vbox.getChildren().addAll(this.armMenuBar.getNode(), this.armToolBar.getNode(), dockPane);
		VBox.setVgrow(dockPane, Priority.ALWAYS);

		//primaryStage.show(); // render to avoid node.lookup() to fail

		this.scene = new Scene(vbox, 800, 500);
		primaryStage.setScene(this.scene);
		primaryStage.sizeToScene();

		setButtonEvents();
		setExecutionMode();

		// test the look and feel with both Caspian and Modena
		Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);

		newUpdateThread(this.running);
		
		primaryStage.show();
		
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
            	System.exit(0);
            }
        });


		// initialize the default styles for the dock pane and undocked nodes using the
		// DockFX
		// library's internal Default.css stylesheet
		// unlike other custom control libraries this allows the user to override them
		// globally
		// using the style manager just as they can with internal JavaFX controls
		// this must be called after the primary stage is shown
		// https://bugs.openjdk.java.net/browse/JDK-8132900
		DockPane.initializeDefaultUserAgentStylesheet();
	}

	private void setExecutionMode() {
		this.codeEditor.setExecutionMode(this.executionMode);
		this.armMenuBar.setExecutionMode(this.executionMode);
		this.armToolBar.setExecutionMode(this.executionMode);
	}

	private void setButtonEvents() {
		//MENU BAR
		//file
		this.armMenuBar.getNeW().setOnAction(actionEvent -> {
			/*if(!this.codeEditor.getProgramAsString().equals("")){
				warningPopup("Unsaved work will be lost");
			}*/
			this.codeEditor.setProgramAsString("");
			this.currentProgramPath = null;
		});
		this.armMenuBar.getOpenFile().setOnAction(actionEvent -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Open Program");
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Assembly Files", "*.S"), new FileChooser.ExtensionFilter("ARMStrong Files", "*.ARMS") );

			File chosenFile = fileChooser.showOpenDialog(this.stage);
			if (chosenFile != null){
				try {
					this.codeEditor.setProgramAsString(new String(Files.readAllBytes(Paths.get(chosenFile.getAbsolutePath())), "UTF-8"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.currentProgramPath = chosenFile;
			}
		});
		this.armMenuBar.getSave().setOnAction(actionEvent -> {
			if(this.currentProgramPath == null){
				this.armMenuBar.getSaveAs().fire();
			}
			saveFile(codeEditor.getProgramAsString(), currentProgramPath);
		});
		this.armMenuBar.getSaveAs().setOnAction(actionEvent -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save assembly program");
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Assembly Files", "*.S"), new FileChooser.ExtensionFilter("ARMStrong Files", "*.ARMS") );
			File chosenFile = fileChooser.showSaveDialog(stage);
			saveFile(codeEditor.getProgramAsString(), chosenFile);
			this.currentProgramPath = chosenFile;
		});
		//window
		this.armMenuBar.getNewMemoryWindow().setOnAction(actionEvent -> {
			RamView moreRamView = new RamView(simulator);
			this.ramViews.add(moreRamView);
			moreRamView.getNode().dock(dockPane, DockPos.RIGHT);
		});
		this.armMenuBar.getNewRegistersWindow().setOnAction(actionEvent -> {
			RegistersView moreRegistersView = new RegistersView(simulator);
			this.registersViews.add(moreRegistersView);
			moreRegistersView.getNode().dock(dockPane, DockPos.LEFT);
		});
		this.armMenuBar.getNewLedGame().setOnAction(actionEvent -> {
			LedView moreLedView = new LedView();
			//this.ledViews.add(moreLedView);
			moreLedView.getNode().dock(dockPane, DockPos.RIGHT);
		});
		this.armMenuBar.getPreferences().setOnAction(actionEvent -> {
			LedView moreLedView = new LedView();
			//this.ledViews.add(moreLedView);
			moreLedView.getNode().dock(dockPane, DockPos.RIGHT);
		});

		//Run
		this.armMenuBar.getSwitchMode().setOnAction(actionEvent -> {
			if (!running.get()) {
				executionMode = !executionMode;
				if (executionMode) {
					try {
						simulator.setProgram(codeEditor.getProgramAsString());
					} catch (Exception e) {
						System.out.println(e.getMessage());
						executionMode = !executionMode;
					}
				}
				setExecutionMode();
			}
		});
		this.armMenuBar.getRunMenuItem().setOnAction(actionEvent -> {
			if (executionMode && !(running.get())) {
				new Thread(() -> {
					this.running.set(true);

					this.simulator.run();

					this.running.set(false);

					updateUI();
				}).start();
			}
		});
		this.armMenuBar.getRunStepMenuItem().setOnAction(actionEvent -> {
			if (executionMode && !(running.get())) {
				new Thread(() -> {
					this.running.set(true);

					this.simulator.runStep();

					this.running.set(false);

					updateUI();
				}).start();
			}
		});
		this.armMenuBar.getStopMenuItem().setOnAction(actionEvent -> {
			simulator.interruptExecutionFlow();
			updateUI();
			this.running.set(false);
		});
		this.armMenuBar.getReloadMenuItem().setOnAction(actionEvent -> {
			if (!running.get()) {
				this.simulator.resetState();
				this.simulator.setProgram(codeEditor.getProgramAsString());
				updateUI();
			}
		});


		//TOOL BAR
		this.armToolBar.getSwitchButton().setOnAction(actionEvent -> armMenuBar.getSwitchMode().fire());
		this.armToolBar.getRunButton().setOnAction(actionEvent -> armMenuBar.getRunMenuItem().fire());
		this.armToolBar.getStepByStepButton().setOnAction(actionEvent -> armMenuBar.getRunStepMenuItem().fire());
		this.armToolBar.getReloadButton().setOnAction(actionEvent -> armMenuBar.getReloadMenuItem().fire());
		this.armToolBar.getStopButton().setOnAction(actionEvent -> armMenuBar.getStopMenuItem().fire());

		// the console
		this.consoleView.getTextField().setOnKeyPressed((KeyEvent ke) -> {
			if (ke.getCode().equals(KeyCode.ENTER)) {
				simulator.setConsoleInput(this.consoleView.getTextField().getText());
				this.consoleView.getTextField().clear();
			}
		});

		//the keyboard shortcuts
		this.scene.setOnKeyPressed((KeyEvent ke) -> {
			if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(ke)) {
				this.armMenuBar.getSave().fire();
			}
			if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN,KeyCombination.SHIFT_DOWN).match(ke)) {
				this.armMenuBar.getSaveAs().fire();
			}
			if (new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN).match(ke)) {
				this.armMenuBar.getOpenFile().fire();
			}
			if (new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN).match(ke)) {
				//preferences
			}
			if (new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN).match(ke)) {
				this.armMenuBar.getNeW().fire();
			}
			if (new KeyCodeCombination(KeyCode.F5).match(ke)) {
				this.armMenuBar.getRunMenuItem().fire();
			}
			if (new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN).match(ke)) {
				this.armMenuBar.getSwitchMode().fire();
			}
			if (new KeyCodeCombination(KeyCode.F11).match(ke)) {
				this.armMenuBar.getRunStepMenuItem().fire();
			}
		});
	}

	private void updateUI() {
		Platform.runLater(() -> {
			for (RegistersView registerView : this.registersViews) {
				registerView.updateRegisters();
			}
			
			this.codeEditor.highlightLine(this.simulator.getCurrentLine());
		});
	}
	
	private void newUpdateThread(AtomicBoolean runningFlag) {
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				
				if (!runningFlag.get()) {
					continue;
				}
				
				updateUI();
			}
		}).start();
	}

	private void saveFile(String content, File theFile) {
		try (FileWriter outputStream = new FileWriter(theFile)) {
			outputStream.write(content);
			//stage.setTitle("#@RM - " + theFile.getName());
		} catch (IOException | NullPointerException e) {

		}
	}

	/*private void warningPopup(String message){
		final Stage warningStage = new Stage();
		warningStage.setTitle("Warning");
		Image applicationIcon = new Image("file:warning.png");
		warningStage.getIcons().add(applicationIcon);

		warningStage.initModality(Modality.APPLICATION_MODAL);
		warningStage.initOwner(this.stage);

		warningStage.getIcons().add(applicationIcon);
		warningStage.setTitle("About - #@RMStrong");
		try {
			Pane main = FXMLLoader.load(getClass().getResource("/resources/warning.fxml"));
			warningStage.setScene(new Scene(main, 500, 280));
			Text messageText = (Text) main.lookup("#message");
			messageText.setText(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		warningStage.show();
	}*/
}
