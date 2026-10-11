package com.threattrace.analysis;
import com.threattrace.domain.InvestigationException;
import com.threattrace.domain.Result;
import com.threattrace.persistence.CaseRepository;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;
@Service
public class AnalysisJobService {
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,4,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"threattrace-analysis");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private final CaseRepository repository;private final SimilarityStrategy strategy;
    public AnalysisJobService(CaseRepository repository,SimilarityStrategy strategy){this.repository=repository;this.strategy=strategy;}
    public Result<Map<String,Object>> compare(int a,int b){try{Future<Map<String,Object>> f=workers.submit(()->strategy.compare(repository.analyticsFeatures(a),repository.analyticsFeatures(b)));return Result.success(f.get(8,TimeUnit.SECONDS));}catch(TimeoutException e){throw new InvestigationException("Comparison exceeded the 8 second analysis limit",e);}catch(RejectedExecutionException e){throw new InvestigationException("Analysis queue is full; retry shortly",e);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new InvestigationException("Comparison interrupted",e);}catch(ExecutionException e){throw new InvestigationException("Comparison failed: "+e.getCause().getMessage(),e.getCause());}}
    @PreDestroy public void close(){workers.shutdown();}
}
