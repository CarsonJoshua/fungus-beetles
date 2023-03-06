import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Game extends JPanel {
    final int GAME_WIDTH;
    final int GAME_HEIGHT;
    final int TILE_WIDTH;
    final int TILE_HEIGHT;
    int[][] nutrientWell;
    final static int[] not4 = {0,1,2,3,5,6,7,8};
    Fungus[][] fungusGrid;
    ArrayList<Fungus> fungusCores;
//    Beetle[][] beetleGrid;
    ArrayList<Beetle> activeBeetles;
    ArrayList<Fungus> orphanedFungus;

    final int STARTING_SPORES=2;
    final int STARTING_BEETLES=1;
    final int MAX_STARTING_NUTRIENTS=20;
    final int STARTING_BEETLE_NUTRITION=15;
    final int SPORE_POINT=10;
    final int BEETLE_POINT=40;
    final int TURN_TIME = 500;
    final int FUNGUS_SPREAD_RESIST = 3;
    final boolean INFINITE = true;

    public static void main(String[] args) {
        int gameWidth = 200;
        int gameHeight = 200;
        int tileWidth = 3;
        int tileHeight = 3;
        int maxGameTurns = 1000;

        JFrame frame = new JFrame();
        frame.setSize((gameWidth+1)*tileWidth, (gameHeight+4)*tileHeight);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Game game = new Game(gameWidth, gameHeight, tileWidth, tileHeight);
        frame.add(game);
        frame.setVisible(true);
        for(int t = 0; t < maxGameTurns; t++){
            game.gameCycle();
            game.repaint();//test if needed TODO
            System.out.println("Processing turn "+t);
        }
    }

    public Game(int gameWidth, int gameHeight, int tileWidth, int tileHeight) {
        GAME_WIDTH = gameWidth;
        GAME_HEIGHT = gameHeight;
        TILE_WIDTH = tileWidth;
        TILE_HEIGHT = tileHeight;
        nutrientWell = new int[GAME_WIDTH][GAME_HEIGHT];
        fungusGrid = new Fungus[GAME_WIDTH][GAME_HEIGHT];
        fungusCores = new ArrayList<>();
//        beetleGrid = new Beetle[GAME_WIDTH][GAME_HEIGHT];
        activeBeetles = new ArrayList<>();
        orphanedFungus = new ArrayList<>();
        populate();
        setVisible(true);
    }
    void gameCycle(){
//        level(nutrientWell);
        feedFungus();
        levelFungus();
        growFungus();
        moveBeetles();
        try {
            TimeUnit.MILLISECONDS.sleep(TURN_TIME);
        }catch (InterruptedException ie){//TODO make this less hacky
            ie.printStackTrace();
        }
        if(INFINITE){
            if(activeBeetles.isEmpty() && STARTING_BEETLES>0)
                activeBeetles.add(newBeetle());
            if(fungusCores.isEmpty() && STARTING_SPORES>0)
                spore();
        }
    }
    void level(int[][] arr){
        level(arr, 0, 0, 0, 1);
        level(arr, 0, 0, 1, 0);
        for(int i = 1; i < GAME_WIDTH-1; i++){
            level(arr, i, 0, i, 1);
            level(arr, i, 0, i+1, 0);
            level(arr, i, 0, i-1, 0);
        }
        level(arr, GAME_WIDTH-1, 0, GAME_WIDTH-2, 0);
        level(arr, GAME_WIDTH-1, 0, GAME_WIDTH-1, 1);
        for(int j = 1; j < GAME_HEIGHT-1; j++){
            level(arr, 0, j, 0, j-1);
            level(arr, 0, j, 1, j);
            level(arr, 0, j, 0, j+1);
            for(int i = 1; i < GAME_WIDTH-1; i++){
                level(arr, i, j, i, j+1);
                level(arr, i, j, i+1, j);
                level(arr, i, j, i, j-1);
                level(arr, i, j, i-1, j);
            }
            level(arr, GAME_WIDTH-1, j, GAME_WIDTH-1, j-1);
            level(arr, GAME_WIDTH-1, j, GAME_WIDTH-1, j+1);
            level(arr, GAME_WIDTH-1, j, GAME_WIDTH-2, j);
        }
        level(arr, 0, GAME_HEIGHT-1, 1, GAME_HEIGHT-1);
        level(arr, 0, GAME_HEIGHT-1, 0, GAME_HEIGHT-2);
        for(int i = 1; i < GAME_WIDTH-1; i++){
            level(arr, i, GAME_HEIGHT-1, i+1, GAME_HEIGHT-1);
            level(arr, i, GAME_HEIGHT-1, i, GAME_HEIGHT-2);
            level(arr, i, GAME_HEIGHT-1, i-1, GAME_HEIGHT-1);
        }
        level(arr, GAME_WIDTH-1, GAME_HEIGHT-1, GAME_WIDTH-1, GAME_HEIGHT-2);
        level(arr, GAME_WIDTH-1, GAME_HEIGHT-1, GAME_WIDTH-2, GAME_HEIGHT-1);
    }
    void level(int[][] arr, int x1, int y1, int x2, int y2){
        if(arr[x1][y1] > arr[x2][y2]+1){
            arr[x1][y1]--;
            arr[x1][y1]++;
        }
    }
    void addRandomNutrition(int n){
        for(int i = 0; i < n; i++){
            nutrientWell[(int)(Math.random()*GAME_WIDTH)][(int)(Math.random()*GAME_HEIGHT)]++;
        }
    }
    void populate(){
        populate(MAX_STARTING_NUTRIENTS, STARTING_SPORES, STARTING_BEETLES);
    }
    void populate(int nutrientMax, int numSpores, int numBeetles){//TODO rewrite to use global variables
        for(int i = 0; i < GAME_WIDTH; i++){
            for(int j = 0; j < GAME_HEIGHT; j++){
                nutrientWell[i][j] = (int)(Math.random()*(nutrientMax+1));
            }
        }
        if(numSpores>GAME_WIDTH*GAME_HEIGHT||numBeetles>GAME_WIDTH*GAME_HEIGHT)throw new RuntimeException("Populating too many mushrooms or beetles");
        for(int i = 0; i < numSpores; i++){
            spore();
        }
        for(int i = 0; i < numBeetles; i++){
            activeBeetles.add(newBeetle());
        }
    }
    void feedFungus(){
        for(Fungus c:fungusCores){
            feed(c);
        }
    }
    void feed(Fungus f){
        if(nutrientWell[f.x][f.y]>0){
            nutrientWell[f.x][f.y]--;
            f.nutrition++;
        }
        for(Fungus ch:f.children){
            feed(ch);
        }
    }
    void levelFungus(){
        for(Fungus f:fungusCores){
            for(Fungus ch:f.children){
                if(levelFungus(ch, f.nutrition)){
                    f.nutrition++;
                }
            }
        }
    }
    boolean levelFungus(Fungus f, int parentNutrition){
        for(Fungus ch:f.children){
            if(levelFungus(ch, f.nutrition)){
                f.nutrition++;
            }
        }
        if(f.nutrition>parentNutrition){
            f.nutrition--;
            return true;
        }
        return false;
    }
    void growFungus(){
        for(Fungus f: fungusCores){
            growFungus(f);
        }
        int sporeCounter = 0;
        for(Fungus f:fungusCores){
            if(f.nutrition>SPORE_POINT){
                f.nutrition-=SPORE_POINT;
                sporeCounter++;
                addRandomNutrition(SPORE_POINT-1);
            }
        }
        for(int i = 0; i < sporeCounter; i++)spore();

    }
    void growFungus(Fungus f){
        for(Fungus ch:f.children)
            growFungus(ch);
        if(f.nutrition<=1)return;
        int maxNearNutrition = nutrientWell[f.x][f.y]+FUNGUS_SPREAD_RESIST;
        int maxNearNutritionX = f.x;
        int maxNearNutritionY = f.y;
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                if(nutrientWell[boundX(f.x-1+i)][boundY(f.y-1+j)]>maxNearNutrition&&fungusGrid[boundX(f.x-1+i)][boundY(f.y-1+j)]==null){
                    maxNearNutrition = nutrientWell[boundX(f.x-1+i)][boundY(f.y-1+j)];
                    maxNearNutritionX = boundX(f.x-1+i);
                    maxNearNutritionY = boundY(f.y-1+j);
                }
            }
        }
        if(maxNearNutritionX==f.x&&maxNearNutritionY==f.y)return;
        f.nutrition--;
        f.children.add(fungusGrid[maxNearNutritionX][maxNearNutritionY]=new Fungus(maxNearNutritionX,maxNearNutritionY, f));
    }
    int boundX(int val){return bound(0, GAME_WIDTH-1, val );}
    int boundY(int val){return bound(0, GAME_HEIGHT-1, val);}
    int bound(int lower, int upper, int val){return(Math.max(lower, Math.min(val, upper)));}
    void spore(){
        int x = 0;
        int y = 0;
        do{
            x = (int)(Math.random()*GAME_WIDTH);
            y = (int)(Math.random()*GAME_HEIGHT);
//            System.out.println("Attempting to generate spore at ("+x+","+y+").");
        }while(fungusGrid[x][y]!=null);
        fungusGrid[x][y] = new Fungus(x, y, null);
        fungusCores.add(fungusGrid[x][y]);
    }
    void moveBeetles(){//TODO trying to avoid concurrentModificationException here, maybe a better way?
        ArrayList<Beetle> dyingBeetles = new ArrayList<>();
        ArrayList<Beetle> newBeetles = new ArrayList<>();
        for(Beetle b: activeBeetles){
            if(b.nutrition>BEETLE_POINT){
                newBeetles.add(newBeetle(b));
            }else if(moveBeetle(b))dyingBeetles.add(b);
        }
        for(Beetle dB:dyingBeetles){
            activeBeetles.remove(dB);
        }
        for(Beetle nB:newBeetles){
            activeBeetles.add(nB);
        }
    }
    Beetle newBeetle(Beetle parent){
        int dir = 0;
        do{
            dir = not4[(int)(Math.random()*not4.length)];
        }while(boundX(parent.x-1+(dir%3))==parent.x && boundY(parent.y-1+(dir/3))==parent.y);
        Beetle child = new Beetle(boundX(parent.x-1+(dir%3)),boundY(parent.y-1+(dir/3)), parent.nutrition-(parent.nutrition/2));
        parent.nutrition=parent.nutrition/2;
        eatFungus(child);
        return child;
    }
    Beetle newBeetle(){
        return new Beetle((int)(Math.random()*GAME_WIDTH),(int)(Math.random()*GAME_HEIGHT), STARTING_BEETLE_NUTRITION);
    }
    //returns true if beetle is kill
    boolean moveBeetle(Beetle b){
        if(b.nutrition==0)return true;
        int fungusTastiness = 0;
        int fungusX = b.x;
        int fungusY = b.y;
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                if(fungusGrid[boundX(b.x-1+i)][boundY(b.y-1+j)]!=null && fungusGrid[boundX(b.x-1+i)][boundY(b.y-1+j)].nutrition>fungusTastiness){
                    fungusTastiness=fungusGrid[boundX(b.x-1+i)][boundY(b.y-1+j)].nutrition;
                    fungusX=boundX(b.x-1+i);
                    fungusY=boundY(b.y-1+j);
                }
            }
        }
        if(fungusTastiness == 0){
            int dir = 0;
            do{
                dir = not4[(int)(Math.random()*not4.length)];
            }while(boundX(b.x-1+(dir%3))==b.x && boundY(b.y-1+(dir/3))==b.y);
            nutrientWell[b.x][b.y]++;
            b.nutrition--;
            b.x=boundX(b.x-1+(dir%3));
            b.y=boundY(b.y-1+(dir/3));
            return false;
        }
        nutrientWell[b.x][b.y]++;
        b.nutrition--;
        b.x=fungusX;
        b.y=fungusY;
        eatFungus(b);
        return false;
    }
    void eatFungus(Beetle b){
        Fungus f = fungusGrid[b.x][b.y];
        if(f==null)return;
        for(Fungus ch: f.children){
            ch.parent=null;
            orphanedFungus.add(ch);
        }
        if(f.parent==null)
            fungusCores.remove(f);
        else f.parent.children.remove(f);
        fungusGrid[f.x][f.y]=null;
        b.nutrition += f.nutrition;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for(int i = 0; i < GAME_WIDTH; i++){
            for(int j = 0; j < GAME_HEIGHT; j++){
                g.setColor(new Color(75-nutrientWell[i][j],52-nutrientWell[i][j],42));
                g.fillRect(i*TILE_WIDTH, j*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
//                if(fungusGrid[i][j]!=null){
//                    if(fungusCores.contains(fungusGrid[i][j]))
//                        g.setColor(Color.MAGENTA);
//                    else
////                        g.setColor(Color.PINK);
//                        g.setColor(fungusGrid[i][j].color);
//                    g.fillRect(i*TILE_WIDTH, j*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
////                    System.out.println("Drawing fungus at ("+i+","+j+").");
//                }

//                if(beetleGrid[i][j]!=null){
//                    g.setColor(Color.CYAN);
//                    g.fillRect(i*TILE_WIDTH, j*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
//                }

            }
        }
        for(Fungus f:fungusCores) paintFungusCore(g, f);
        for(Fungus f:orphanedFungus) paintFungus(g, f);
        g.setColor(Color.BLACK);
        for(Beetle b: activeBeetles){
            g.fillRect(b.x*TILE_WIDTH, b.y*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
        }
    }
    protected void paintFungusCore(Graphics g, Fungus f){
        for(Fungus ch:f.children){
            paintFungus(g, ch);
        }
        g.setColor(f.color.darker());
        g.fillRect(f.x*TILE_WIDTH, f.y*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
    }
    protected void paintFungus(Graphics g, Fungus f){
        for(Fungus ch:f.children){
            paintFungus(g, ch);
        }
        g.setColor(f.color);
        g.fillRect(f.x*TILE_WIDTH, f.y*TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);

    }

    class Fungus{
        int x,y;
        int nutrition;
        ArrayList<Fungus> children;
        Fungus parent;
        Color color;
        public Fungus(int x, int y, Fungus parent){
            this.x = x;
            this.y = y;
            this.nutrition = 1;
            children = new ArrayList<>();
            this.parent = parent;
            if(parent==null){
                color = new Color((int)(Math.random()*256),(int)(Math.random()*256),(int)(Math.random()*256));
            }else{
                color = parent.color;
            }
        }
    }
    class Beetle{
        int x,y;
        int nutrition;

        public Beetle(int x, int y, int nutrition) {
            this.x = x;
            this.y = y;
            this.nutrition = nutrition;
        }
    }
}