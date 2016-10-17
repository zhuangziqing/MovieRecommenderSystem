
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobHistory.Values;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
	

public class Multiplication {
	public static class MultiplicationMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {
		
		/*
		 * (non-Javadoc)
		 * @see org.apache.hadoop.mapreduce.Mapper#setup(org.apache.hadoop.mapreduce.Mapper.Context)
		 * [movie 1 {movie1, movie2, 8} {movie1, movie3, 5} {} 
		 * [movie2 {movie2, movie1, 8} {} ] 
		 * 
		 */
		//For movieRelationMap, we want to make a hashmap
		//key as movie1   value as movie1 and others co-occurrence count.
		//[movie 1 {movie1, movie2, 8} {movie1, movie3, 5} {}
		Map<Integer, List<MovieRelation>> movieRelationMap = new HashMap<>();
		//For denominator, we want to make a map 
		//key: movie1   value as sum of all co-occurrence count.
		Map<Integer, Integer> denominator = new HashMap<>();
		
		@Override
		public void setup(Context context) throws IOException {
			//initial input   movieA : movieB \t relation
			//We want to get movieRelationMap and denominator in the setup method.
			Configuration conf = context.getConfiguration();
			String filePath = conf.get("coOccurrencePath","/coOccurrenceMatrix/part-r-00000");
			Path pt = new Path(filePath);
			FileSystem fs = FileSystem.get(conf);
			BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(pt)));
			String line = br.readLine();
			
			while(line != null) {
				//movieA : movieB \t relation
				String[] tokens = line.toString().trim().split("\t");
				String[] movies = tokens[0].split(":");
				int movie1 = Integer.parseInt(movies[0]);
				int movie2 = Integer.parseInt(movies[1]);
				int relation = Integer.parseInt(tokens[1]);
				
				//make co-occurrence matrix.
				MovieRelation movieRelation = new MovieRelation(movie1, movie2, relation);
				if(movieRelationMap.containsKey(movie1)) {
					movieRelationMap.get(movie1).add(movieRelation);
				} else {
					List<MovieRelation> list = new ArrayList<MovieRelation>();
					list.add(movieRelation);
					movieRelationMap.put(movie1,list);
				}
				line = br.readLine();
			}
			br.close();
			
			//loop to get the sum of co-occurrence.
			for(Map.Entry<Integer, List<MovieRelation>> entry : movieRelationMap.entrySet()) {
				int sum = 0;
				for(MovieRelation relation : entry.getValue()) {
					sum += relation.getRelation();
				}
				denominator.put(entry.getKey(), sum);
			}
		}
		
		
		@Override
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			//initial input: user, movie, rating
			//output: user:movie  score
			String[] tokens = value.toString().trim().split(",");
			int user = Integer.parseInt(tokens[0]);
			int movie = Integer.parseInt(tokens[1]);
			double rating = Double.parseDouble(tokens[2]);
			
			for(MovieRelation relation : movieRelationMap.get(movie)) {
				double score = rating * relation.getRelation(); // 5 * 8 = 40  -> normalize
				//map.get(movie2) / sum
				// calculate the sum to normalize matrix
				score = score / denominator.get(relation.getMovie2());
				DecimalFormat df = new DecimalFormat("#.00");
				score = Double.valueOf(df.format(score));
				context.write(new Text(user + ":" + relation.getMovie2()), new DoubleWritable( score));
				//user movietag: score
				//we can optimize to key -> user+movietag  value
				
			}
		}
	}

	public static class MultiplicationReducer extends
			Reducer<Text, DoubleWritable, IntWritable, Text> {
		
		public void reduce(Text key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			
			//input: key user:movie2 value score
			//output: key user   value the sum of movie2's score
			double sum = 0;
			while(values.iterator().hasNext()) {
				sum += values.iterator().next().get();
			}
			String[] tokens = key.toString().split(":");
			int user = Integer.parseInt(tokens[0]);
			context.write(new IntWritable(user), new Text(tokens[1] + ":" + sum));
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.set("coOccurrencePath", args[0]);
		Job job = Job.getInstance();
		job.setMapperClass(MultiplicationMapper.class);
		job.setReducerClass(MultiplicationReducer.class);
		
		job.setJarByClass(Multiplication.class);
		// set number of reducer 3 if you want.
		//job.setNumReduceTasks(3);
		
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(DoubleWritable.class);
		
		TextInputFormat.setInputPaths(job, new Path(args[1]));
		TextOutputFormat.setOutputPath(job, new Path(args[2]));
		
		job.waitForCompletion(true);
	}
}

